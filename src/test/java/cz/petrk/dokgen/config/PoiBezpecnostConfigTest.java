package cz.petrk.dokgen.config;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Overuje, ze @PostConstruct opravdu nastavi limity pro ochranu proti
 * "zip bombe" (viz trida) na ocekavane hodnoty - bez tohohle testu by
 * preklep v hodnote nebo smazani metody ZipSecureFile.setX() nikdo neodhalil,
 * dokud by nekdo nenahral skutecne zlomyslny .docx soubor.
 */
class PoiBezpecnostConfigTest {

    @AfterEach
    void obnovVychoziHodnoty() {
        // Staticky stav ZipSecureFile je sdileny napric celou JVM - vratit na
        // vychozi hodnoty, aby tento test neovlivnil jine testy bezici ve stejnem behu.
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(0xFFFFFFFFL);
    }

    @Test
    void nastaviLimityProZip() {
        ZipSecureFile.setMinInflateRatio(0.5d);
        ZipSecureFile.setMaxEntrySize(1L);

        new PoiBezpecnostConfig().nastavLimityProZip();

        assertThat(ZipSecureFile.getMinInflateRatio()).isEqualTo(0.01d);
        assertThat(ZipSecureFile.getMaxEntrySize()).isEqualTo(100L * 1024 * 1024);
    }
}
