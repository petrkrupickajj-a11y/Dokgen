package cz.petrk.dokgen.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolozkyVypocetServiceTest {

    private PolozkyVypocetService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        service = new PolozkyVypocetService(zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void spoctiPrazdnySeznamVratiPrazdnePolozkyANulovyCelkem() {
        PolozkyVypocetService.Vysledek vysledek = service.spocti(List.of());

        assertThat(vysledek.polozky()).isEmpty();
        assertThat(vysledek.celkem()).isEqualTo("0,00");
    }

    @Test
    void spoctiJednuPolozkuSpocitaCelkemJakoMnozstviKratCena() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("Konzultace", new BigDecimal("2"), new BigDecimal("1250.50"));

        PolozkyVypocetService.Vysledek vysledek = service.spocti(List.of(vstup));

        Map<String, String> radek = vysledek.polozky().get(0);
        assertThat(radek.get("poradi")).isEqualTo("1");
        assertThat(radek.get("nazev")).isEqualTo("Konzultace");
        assertThat(radek.get("cena")).isEqualTo("1 250,50");
        assertThat(radek.get("celkem")).isEqualTo("2 501,00");
        assertThat(vysledek.celkem()).isEqualTo("2 501,00");
    }

    @Test
    void spoctiVicePolozekSectePresVsechnySDesetinnymiHodnotami() {
        var polozka1 = new PolozkyVypocetService.PolozkaVstup("A", new BigDecimal("2"), new BigDecimal("1250.50"));
        var polozka2 = new PolozkyVypocetService.PolozkaVstup("B", new BigDecimal("1"), new BigDecimal("99.90"));

        PolozkyVypocetService.Vysledek vysledek = service.spocti(List.of(polozka1, polozka2));

        // 2 * 1250,50 + 1 * 99,90 = 2600,90
        assertThat(vysledek.celkem()).isEqualTo("2 600,90");
        assertThat(vysledek.polozky()).hasSize(2);
        assertThat(vysledek.polozky().get(0).get("poradi")).isEqualTo("1");
        assertThat(vysledek.polozky().get(1).get("poradi")).isEqualTo("2");
    }

    @Test
    void spoctiZaokrouhlujeNaDvaDesetinneMistaHalfUp() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("Materiál", new BigDecimal("3"), new BigDecimal("0.005"));

        PolozkyVypocetService.Vysledek vysledek = service.spocti(List.of(vstup));

        // 3 * 0,005 = 0,015 -> zaokrouhleno HALF_UP na 0,02
        assertThat(vysledek.polozky().get(0).get("celkem")).isEqualTo("0,02");
    }

    @Test
    void spoctiSPrazdnymNazvemVyhodiChybu() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("  ", BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> service.spocti(List.of(vstup)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Název položky je povinný");
    }

    @Test
    void spoctiSeZapornymMnozstvimVyhodiChybu() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("Konzultace", new BigDecimal("-1"), BigDecimal.TEN);

        assertThatThrownBy(() -> service.spocti(List.of(vstup)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Množství položky nesmí být záporné");
    }

    @Test
    void spoctiSeZapornouCenouVyhodiChybu() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("Konzultace", BigDecimal.ONE, new BigDecimal("-10"));

        assertThatThrownBy(() -> service.spocti(List.of(vstup)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cena položky nesmí být záporná");
    }

    @Test
    void spoctiSNulovouCenouANulovymMnozstvimNevyhodiChybu() {
        var vstup = new PolozkyVypocetService.PolozkaVstup("Dárek zdarma", BigDecimal.ZERO, BigDecimal.ZERO);

        PolozkyVypocetService.Vysledek vysledek = service.spocti(List.of(vstup));

        assertThat(vysledek.polozky().get(0).get("celkem")).isEqualTo("0,00");
    }
}
