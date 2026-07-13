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
