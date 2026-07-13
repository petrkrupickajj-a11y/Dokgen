package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class GenerovaneDokumentyUklidRunnerTest {

    @TempDir
    Path uloznyAdresar;

    private VygenerovanyDokumentRepository repository;
    private VygenerovanyDokumentUlozisteService uloziste;
    private GenerovaneDokumentyUklidRunner runner;

    @BeforeEach
    void setUp() throws IOException {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        repository = Mockito.mock(VygenerovanyDokumentRepository.class);
        uloziste = new VygenerovanyDokumentUlozisteService(uloznyAdresar.toString(), zpravy);
        runner = new GenerovaneDokumentyUklidRunner(repository, uloziste, 90);
    }

    private VygenerovanyDokument zaznam(long id) {
        VygenerovanyDokument zaznam = new VygenerovanyDokument(1L, "Jan Novák", "Smlouva", "PDF");
        ReflectionTestUtils.setField(zaznam, "id", id);
        return zaznam;
    }

    @Test
    void smazeSouborStarehoZaznamu() throws IOException {
        uloziste.uloz(42L, "PDF", "obsah".getBytes());
        given(repository.findAllByVytvorenoDneBefore(any(LocalDateTime.class))).willReturn(List.of(zaznam(42L)));

        runner.run(new DefaultApplicationArguments());

        assertThat(uloziste.existuje(42L, "PDF")).isFalse();
    }

    @Test
    void nesahaNaSouboryMimoStareZaznamy() throws IOException {
        uloziste.uloz(7L, "WORD", "obsah".getBytes());
        given(repository.findAllByVytvorenoDneBefore(any(LocalDateTime.class))).willReturn(List.of());

        runner.run(new DefaultApplicationArguments());

        assertThat(uloziste.existuje(7L, "WORD")).isTrue();
    }

    @Test
    void chybejiciSouborStarehoZaznamuNevyhodiChybu() {
        given(repository.findAllByVytvorenoDneBefore(any(LocalDateTime.class))).willReturn(List.of(zaznam(999L)));

        runner.run(new DefaultApplicationArguments());
        // zadna vyjimka - smaz() na neexistujicim souboru je no-op (Files.deleteIfExists)
    }

    @Test
    void naplanovanyUklidDelaTotezJakoPriStartu() throws IOException {
        uloziste.uloz(5L, "PDF", "obsah".getBytes());
        given(repository.findAllByVytvorenoDneBefore(any(LocalDateTime.class))).willReturn(List.of(zaznam(5L)));

        runner.uklidNaplanovane();

        assertThat(uloziste.existuje(5L, "PDF")).isFalse();
    }
}
