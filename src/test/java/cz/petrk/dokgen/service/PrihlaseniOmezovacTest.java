package cz.petrk.dokgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PrihlaseniOmezovacTest {

    private TestovaciHodiny hodiny;
    private PrihlaseniOmezovac omezovac;

    @BeforeEach
    void setUp() {
        hodiny = new TestovaciHodiny(Instant.parse("2026-01-01T10:00:00Z"));
        omezovac = new PrihlaseniOmezovac(hodiny);
    }

    @Test
    void novyUcetNeniZamceny() {
        assertThat(omezovac.jeZamceno("admin")).isFalse();
    }

    @Test
    void poCtyrechNeuspesnychPokusechJesteNeniZamceno() {
        for (int i = 0; i < 4; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }

        assertThat(omezovac.jeZamceno("admin")).isFalse();
    }

    @Test
    void poPatemNeuspesnemPokusuJeZamceno() {
        for (int i = 0; i < 5; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }

        assertThat(omezovac.jeZamceno("admin")).isTrue();
    }

    @Test
    void uspesnePrihlaseniVynulujePocitadloNeuspechu() {
        for (int i = 0; i < 4; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }
        omezovac.zaznamenejUspech("admin");
        omezovac.zaznamenejNeuspech("admin");

        assertThat(omezovac.jeZamceno("admin")).isFalse();
    }

    @Test
    void zamceniVyprsiPoUplynutiDobyZamceni() {
        for (int i = 0; i < 5; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }
        assertThat(omezovac.jeZamceno("admin")).isTrue();

        hodiny.posunOMinut(16);

        assertThat(omezovac.jeZamceno("admin")).isFalse();
    }

    @Test
    void jmenoJePripadNecitliveAOriznuteOMezery() {
        for (int i = 0; i < 5; i++) {
            omezovac.zaznamenejNeuspech("  Admin  ");
        }

        assertThat(omezovac.jeZamceno("admin")).isTrue();
    }

    @Test
    void ruznaJmenaSeNavzajemNeovlivnuji() {
        for (int i = 0; i < 5; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }

        assertThat(omezovac.jeZamceno("asistentka")).isFalse();
    }

    @Test
    void starePromazeAzPriDalsimZapisu() {
        // Jeden neuspech pro "admin" - zaznam zustane, dokud neuplyne dvojnasobek DOBA_ZAMCENI.
        omezovac.zaznamenejNeuspech("admin");
        assertThat(omezovac.pocetZaznamu()).isEqualTo(1);

        hodiny.posunOMinut(29);
        omezovac.zaznamenejNeuspech("asistentka");
        assertThat(omezovac.pocetZaznamu()).isEqualTo(2);

        // Po 31 minutach (> 2x DOBA_ZAMCENI = 30 min) od posledni aktivity "admin"
        // uz jeho zaznam nema na vysledek zadny vliv - dalsi zapis (pro jiny email) ho promaze.
        hodiny.posunOMinut(2);
        omezovac.zaznamenejNeuspech("treti-ucet");

        assertThat(omezovac.pocetZaznamu()).isEqualTo(2);
    }

    @Test
    void aktivniZaznamSeNepromazeAniPriUklidu() {
        for (int i = 0; i < 5; i++) {
            omezovac.zaznamenejNeuspech("admin");
        }
        assertThat(omezovac.jeZamceno("admin")).isTrue();

        // 10 minut < DOBA_ZAMCENI (15 min), zamek jeste trva
        hodiny.posunOMinut(10);
        omezovac.zaznamenejNeuspech("jiny-ucet");

        assertThat(omezovac.jeZamceno("admin")).isTrue();
        assertThat(omezovac.pocetZaznamu()).isEqualTo(2);
    }

    /** Testovaci Clock s Instant, ktery se da v testu rucne posunout dopredu v case. */
    private static final class TestovaciHodiny extends Clock {
        private Instant cas;

        private TestovaciHodiny(Instant cas) {
            this.cas = cas;
        }

        void posunOMinut(long minuty) {
            cas = cas.plus(Duration.ofMinutes(minuty));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return cas;
        }
    }
}
