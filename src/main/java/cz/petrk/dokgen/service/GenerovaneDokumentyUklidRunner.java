package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Uklizi stare vygenerovane dokumenty z disku (./data/generated-documents) -
 * bez tohohle by slozka rostla do nekonecna, protoze kazde vygenerovani
 * vytvori novy soubor a appka ho jinak nikdy nemaze (viz README, sekce
 * /historie - "Known limitation").
 *
 * Mazou se jen FYZICKE SOUBORY starsi nez dokgen.vygenerovane-dokumenty.uchovat-dny -
 * audit zaznam v databazi (VygenerovanyDokument) zustava natrvalo, aby historie
 * generovani zustala uplna. Zaznamy bez souboru uz appka umi zobrazit (misto
 * tlacitka "Zobrazit" ukaze "Soubor není dostupný") - stejny princip jako u
 * zaznamu z doby pred zavedenim ukladani souboru.
 *
 * Bezi jednou pri kazdem startu appky (ApplicationRunner - typicky pruben,
 * appka se dnes pouziva hlavne v kratkych relacich pres mvnw spring-boot:run)
 * a navic jednou denne (@Scheduled), kdyby appka bezela dlouhodobe na serveru.
 */
@Component
public class GenerovaneDokumentyUklidRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(GenerovaneDokumentyUklidRunner.class);

    private final VygenerovanyDokumentRepository repository;
    private final VygenerovanyDokumentUlozisteService uloziste;
    private final int uchovatDny;

    public GenerovaneDokumentyUklidRunner(VygenerovanyDokumentRepository repository,
                                           VygenerovanyDokumentUlozisteService uloziste,
                                           @Value("${dokgen.vygenerovane-dokumenty.uchovat-dny:90}") int uchovatDny) {
        this.repository = repository;
        this.uloziste = uloziste;
        this.uchovatDny = uchovatDny;
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
        LocalDateTime hranice = LocalDateTime.now().minusDays(uchovatDny);
        List<VygenerovanyDokument> stareZaznamy = repository.findAllByVytvorenoDneBefore(hranice);

        int smazano = 0;
        for (VygenerovanyDokument zaznam : stareZaznamy) {
            try {
                if (uloziste.smaz(zaznam.getId(), zaznam.getFormat())) {
                    smazano++;
                }
            } catch (IOException e) {
                LOG.warn("Nepodařilo se smazat starý vygenerovaný dokument (id {})", zaznam.getId(), e);
            }
        }

        if (smazano > 0) {
            LOG.info("Úklid starých vygenerovaných dokumentů: smazáno {} souborů starších než {} dní.",
                    smazano, uchovatDny);
        }
    }
}
