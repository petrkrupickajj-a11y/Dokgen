package cz.petrk.dokgen.service;

import cz.petrk.dokgen.repository.ResetHeslaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Uklizi uz pouzite nebo prosle tokeny na reset hesla (entita ResetHesla) -
 * bez tohohle by tabulka rostla do nekonecna, protoze kazda zadost o reset
 * (viz ResetHeslaService.pozadejReset) vytvori novy zaznam a appka ho jinak
 * nikdy nemaze. Na rozdil od GenerovaneDokumentyUklidRunner tu neni co
 * zachovavat pro audit - pouzity/prosly token uz appce k nicemu neni.
 *
 * Bezi jednou pri kazdem startu appky (ApplicationRunner) a navic jednou
 * denne (@Scheduled), kdyby appka bezela dlouhodobe na serveru.
 */
@Component
public class ResetHeslaUklidRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ResetHeslaUklidRunner.class);

    private final ResetHeslaRepository repository;
    private final Clock hodiny;

    public ResetHeslaUklidRunner(ResetHeslaRepository repository, Clock hodiny) {
        this.repository = repository;
        this.hodiny = hodiny;
    }

    // @Transactional je nutne primo na verejne volane metode (ne na privatnim
    // uklid() volanem pres this - self-invokace by Spring @Transactional proxy
    // obesla) - deleteByPouzitTrueOrVyprsiDneBefore je odvozeny "delete" dotaz
    // (ne CrudRepository.delete(...), ktery uz transakci ma vestavenou), takze
    // by bez vlastni transakce spadl na TransactionRequiredException.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        uklid();
    }

    @Scheduled(cron = "${dokgen.uklid.cron:0 0 3 * * *}")
    @Transactional
    public void uklidNaplanovane() {
        uklid();
    }

    // Na rozdil od GenerovaneDokumentyUklidRunner tu neni co delit na jednotlive
    // polozky (jde o jeden hromadny DB dotaz) - kdyby ale selhal (napr. DB soubor
    // docasne uzamceny), try/catch zajisti, ze to jen zaloguje varovani misto
    // shozeni celeho startu appky - viz run() vyse, bezi jako ApplicationRunner.
    private void uklid() {
        try {
            long smazano = repository.deleteByPouzitTrueOrVyprsiDneBefore(LocalDateTime.now(hodiny));
            if (smazano > 0) {
                LOG.info("Úklid tokenů na reset hesla: smazáno {} použitých nebo prošlých záznamů.", smazano);
            }
        } catch (RuntimeException e) {
            LOG.warn("Úklid tokenů na reset hesla selhal.", e);
        }
    }
}
