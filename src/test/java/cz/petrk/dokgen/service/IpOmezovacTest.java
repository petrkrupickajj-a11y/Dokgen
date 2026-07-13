package cz.petrk.dokgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class IpOmezovacTest {

    private TestovaciHodiny hodiny;
    private IpOmezovac omezovac;

    @BeforeEach
    void setUp() {
        hodiny = new TestovaciHodiny(Instant.parse("2026-01-01T10:00:00Z"));
        omezovac = new IpOmezovac(hodiny);
    }

    @Test
    void prvnichPetPozadavkuJePovoleno() {
        for (int i = 0; i < 5; i++) {
            assertThat(omezovac.povolPozadavek("1.2.3.4")).isTrue();
        }
    }

    @Test
    void sestyPozadavekVOknuJeOdmitnut() {
        for (int i = 0; i < 5; i++) {
            omezovac.povolPozadavek("1.2.3.4");
        }

        assertThat(omezovac.povolPozadavek("1.2.3.4")).isFalse();
    }

    @Test
    void poUplynutiOknaSePocitadloVynuluje() {
        for (int i = 0; i < 5; i++) {
            omezovac.povolPozadavek("1.2.3.4");
        }
        assertThat(omezovac.povolPozadavek("1.2.3.4")).isFalse();

        hodiny.posunOMinut(16);

        assertThat(omezovac.povolPozadavek("1.2.3.4")).isTrue();
    }

    @Test
    void ruzneIpAdresySeNavzajemNeovlivnuji() {
        for (int i = 0; i < 5; i++) {
            omezovac.povolPozadavek("1.2.3.4");
        }

        assertThat(omezovac.povolPozadavek("5.6.7.8")).isTrue();
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
