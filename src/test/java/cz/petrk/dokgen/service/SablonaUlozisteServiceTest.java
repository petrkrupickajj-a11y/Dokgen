package cz.petrk.dokgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SablonaUlozisteServiceTest {

    @TempDir
    Path adresar;

    private SablonaUlozisteService uloziste;

    // @TempDir se do pole "adresar" nastavi az po konstruktoru (reflexi), takze
    // uloziste musi vzniknout az tady, ne uz v inicializatoru pole.
    @BeforeEach
    void setUp() throws IOException {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        zpravy.setFallbackToSystemLocale(false);
        uloziste = new SablonaUlozisteService(adresar.toString(), zpravy);
    }

    @Test
    void ulozANactiVratiStejnyObsah() throws IOException {
        byte[] obsah = "obsah sablony".getBytes();

        uloziste.uloz("smlouva.docx", obsah);

        assertThat(uloziste.nacti("smlouva.docx")).isEqualTo(obsah);
        assertThat(adresar.resolve("smlouva.docx")).exists();
    }

    @Test
    void existujeVratiFalsePredUlozenimATruePoUlozeni() throws IOException {
        assertThat(uloziste.existuje("faktura.docx")).isFalse();

        uloziste.uloz("faktura.docx", "x".getBytes());

        assertThat(uloziste.existuje("faktura.docx")).isTrue();
    }

    @Test
    void nactiNeexistujiciSablonyVyhodiChybu() {
        assertThatThrownBy(() -> uloziste.nacti("neexistuje.docx"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("neexistuje.docx");
    }

    @Test
    void ulozPrepisePredchoziObsahStejnehoSouboru() throws IOException {
        uloziste.uloz("nabidka.docx", "puvodni".getBytes());

        uloziste.uloz("nabidka.docx", "novy".getBytes());

        assertThat(uloziste.nacti("nabidka.docx")).isEqualTo("novy".getBytes());
    }

    @Test
    void smazOdstraniSouborZDisku() throws IOException {
        uloziste.uloz("protokol.docx", "x".getBytes());

        uloziste.smaz("protokol.docx");

        assertThat(uloziste.existuje("protokol.docx")).isFalse();
    }

    @Test
    void smazNeexistujicihoSouboruNeselze() {
        assertThatCode(() -> uloziste.smaz("nikdy-neexistoval.docx")).doesNotThrowAnyException();
    }

    // Chovani se lisi podle prostredi (lokalni stroj s GUI vs. headless server/CI) -
    // oba testy dohromady pokryji obe vetve bez ohledu na to, kde se zrovna spousti.
    @Test
    void otevreniKdyzJeDesktopPodporovanANeexistujeSouborVyhodiChybu() {
        assumeTrue(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

        assertThatThrownBy(() -> uloziste.otevriVeVychoziAplikaci("neexistuje.docx"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("neexistuje.docx");
    }

    @Test
    void otevreniKdyzNeniDesktopPodporovanVyhodiSrozumitelnouChybu() {
        assumeFalse(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

        assertThatThrownBy(() -> uloziste.otevriVeVychoziAplikaci("neexistuje.docx"))
                .isInstanceOf(IOException.class);
    }
}
