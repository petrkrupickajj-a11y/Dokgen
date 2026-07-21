package cz.petrk.dokgen.util;

import cz.petrk.dokgen.entity.Klient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KlientDataTest {

    @Test
    void sestavKontextPrevedeVsechnaPoleKlienta() {
        Klient klient = new Klient();
        klient.setJmeno("Jan");
        klient.setPrijmeni("Novák");
        klient.setTelefon("777123456");
        klient.setEmail("jan@example.cz");
        klient.setAdresa("Hlavní 1");
        klient.setMesto("Praha");
        klient.setPsc("11000");
        klient.setIco("12345678");
        klient.setPoznamka("VIP");

        Map<String, String> kontext = KlientData.sestavKontext(klient);

        assertThat(kontext)
                .containsEntry("jmeno", "Jan")
                .containsEntry("prijmeni", "Novák")
                .containsEntry("telefon", "777123456")
                .containsEntry("email", "jan@example.cz")
                .containsEntry("adresa", "Hlavní 1")
                .containsEntry("mesto", "Praha")
                .containsEntry("psc", "11000")
                .containsEntry("ico", "12345678")
                .containsEntry("poznamka", "VIP");
    }

    @Test
    void sestavKontextSNullHodnotamiVratiPrazdneRetezce() {
        Klient klient = new Klient();

        Map<String, String> kontext = KlientData.sestavKontext(klient);

        assertThat(kontext.values()).allMatch(hodnota -> hodnota.equals(""));
    }
}
