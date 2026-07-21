package cz.petrk.dokgen.util;

import cz.petrk.dokgen.config.DodavatelProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DodavatelDataTest {

    @Test
    void sestavKontextPrevedeVsechnaPoleDodavatele() {
        DodavatelProperties dodavatel = new DodavatelProperties();
        dodavatel.setNazev("Coreforge");
        dodavatel.setSidlo("Praha, Česká republika");
        dodavatel.setIco("12345678");
        dodavatel.setCisloUctu("123456789/0100");

        Map<String, String> kontext = DodavatelData.sestavKontext(dodavatel);

        assertThat(kontext)
                .containsEntry("dodavatel.nazev", "Coreforge")
                .containsEntry("dodavatel.sidlo", "Praha, Česká republika")
                .containsEntry("dodavatel.ico", "12345678")
                .containsEntry("dodavatel.cisloUctu", "123456789/0100");
    }

    @Test
    void sestavKontextSNullHodnotamiVratiPrazdneRetezce() {
        DodavatelProperties dodavatel = new DodavatelProperties();

        Map<String, String> kontext = DodavatelData.sestavKontext(dodavatel);

        assertThat(kontext.values()).allMatch(hodnota -> hodnota.equals(""));
    }
}
