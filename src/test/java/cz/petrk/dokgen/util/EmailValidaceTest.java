package cz.petrk.dokgen.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidaceTest {

    @Test
    void platnyEmailJeUznanJakoPlatny() {
        assertThat(EmailValidace.jePlatny("novak@example.com")).isTrue();
    }

    @Test
    void nullNeniPlatny() {
        assertThat(EmailValidace.jePlatny(null)).isFalse();
    }

    @Test
    void retezecBezZaviamaNeniPlatny() {
        assertThat(EmailValidace.jePlatny("novak-example.com")).isFalse();
    }

    @Test
    void retezecBezTeckyVDomeneNeniPlatny() {
        assertThat(EmailValidace.jePlatny("novak@example")).isFalse();
    }

    @Test
    void retezecSMezerouNeniPlatny() {
        assertThat(EmailValidace.jePlatny("novak @example.com")).isFalse();
    }

    @Test
    void normalizujPrevedeNaMalaPismenaAOrizneMezery() {
        assertThat(EmailValidace.normalizuj("  Novak@Example.COM  ")).isEqualTo("novak@example.com");
    }

    @Test
    void normalizujNullVratiPrazdnyRetezec() {
        assertThat(EmailValidace.normalizuj(null)).isEqualTo("");
    }
}
