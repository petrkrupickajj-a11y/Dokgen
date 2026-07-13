package cz.petrk.dokgen.service;

import cz.petrk.dokgen.repository.ResetHeslaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(ApplicationArguments args) {
        uklid();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void uklidNaplanovane() {
        uklid();
    }

    private void uklid() {
        long smazano = repository.deleteByPouzitTrueOrVyprsiDneBefore(LocalDateTime.now(hodiny));
        if (smazano > 0) {
            LOG.info("Úklid tokenů na reset hesla: smazáno {} použitých nebo prošlých záznamů.", smazano);
        }
    }
}
