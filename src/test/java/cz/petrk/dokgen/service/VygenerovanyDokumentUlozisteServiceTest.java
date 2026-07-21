package cz.petrk.dokgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VygenerovanyDokumentUlozisteServiceTest {

    @TempDir
    Path adresar;

    private VygenerovanyDokumentUlozisteService uloziste;

    // @TempDir se do pole "adresar" nastavi az po konstruktoru (reflexi), takze
    // uloziste musi vzniknout az tady, ne uz v inicializatoru pole.
    @BeforeEach
    void setUp() throws IOException {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        zpravy.setFallbackToSystemLocale(false);
        uloziste = new VygenerovanyDokumentUlozisteService(adresar.toString(), zpravy);
    }

    @Test
    void ulozANactiWordDokumentVratiStejnyObsah() throws IOException {
        byte[] obsah = "obsah dokumentu".getBytes();

        uloziste.uloz(1L, "WORD", obsah);

        assertThat(uloziste.nacti(1L, "WORD")).isEqualTo(obsah);
        assertThat(adresar.resolve("1.docx")).exists();
    }

    @Test
    void ulozAPdfDokumentPouzijePriponuPdf() throws IOException {
        byte[] obsah = "pdf obsah".getBytes();

        uloziste.uloz(2L, "PDF", obsah);

        assertThat(adresar.resolve("2.pdf")).exists();
        assertThat(uloziste.nacti(2L, "PDF")).isEqualTo(obsah);
    }

    @Test
    void existujeVratiFalsePredUlozenimATruePoUlozeni() throws IOException {
        assertThat(uloziste.existuje(3L, "WORD")).isFalse();

        uloziste.uloz(3L, "WORD", "x".getBytes());

        assertThat(uloziste.existuje(3L, "WORD")).isTrue();
    }

    @Test
    void nactiNeexistujicihoDokumentuVyhodiChybu() {
        assertThatThrownBy(() -> uloziste.nacti(404L, "WORD"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    @Test
    void smazVratiTrueKdyzSouborExistovalAFalseKdyzUzNe() throws IOException {
        uloziste.uloz(5L, "WORD", "x".getBytes());

        assertThat(uloziste.smaz(5L, "WORD")).isTrue();
        assertThat(uloziste.existuje(5L, "WORD")).isFalse();
        assertThat(uloziste.smaz(5L, "WORD")).isFalse();
    }

    // Chovani se lisi podle prostredi (lokalni stroj s GUI vs. headless server/CI) -
    // oba testy dohromady pokryji obe vetve bez ohledu na to, kde se zrovna spousti.
    @Test
    void otevreniKdyzJeDesktopPodporovanANeexistujeSouborVyhodiChybu() {
        assumeTrue(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

        assertThatThrownBy(() -> uloziste.otevriVeVychoziAplikaci(999L, "WORD"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("999");
    }

    @Test
    void otevreniKdyzNeniDesktopPodporovanVyhodiSrozumitelnouChybu() {
        assumeFalse(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

        assertThatThrownBy(() -> uloziste.otevriVeVychoziAplikaci(999L, "WORD"))
                .isInstanceOf(IOException.class);
    }
}
