package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.repository.SmazanaVestavenaSablonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SablonySeeder pri startu appky zajisti, ze vestavenych 5 sablon existuje
 * jak v databazi, tak na disku - s dvema dulezitymi vyjimkami: nikdy
 * neprepise soubor, ktery uz na disku existuje (uzivatel ho mohl upravit),
 * a nikdy neobnovi sablonu, kterou uzivatel umyslne smazal (tombstone
 * zaznam v SmazanaVestavenaSablonaRepository).
 */
class SablonySeederTest {

    @TempDir
    Path uloznyAdresar;

    private SablonaRepository sablonaRepository;
    private SmazanaVestavenaSablonaRepository smazaneVestaveneRepository;
    private SablonaUlozisteService uloziste;
    private SablonySeeder seeder;

    @BeforeEach
    void setUp() throws IOException {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        zpravy.setFallbackToSystemLocale(false);

        sablonaRepository = Mockito.mock(SablonaRepository.class);
        smazaneVestaveneRepository = Mockito.mock(SmazanaVestavenaSablonaRepository.class);
        uloziste = new SablonaUlozisteService(uloznyAdresar.toString(), zpravy);
        seeder = new SablonySeeder(sablonaRepository, uloziste, smazaneVestaveneRepository);
    }

    @Test
    void prvniStartUlozeVsechPetSouboruAVytvoriVsechPetZaznamu() throws IOException {
        given(sablonaRepository.existsByNazev(any())).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev(any())).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<Sablona> zachycene = ArgumentCaptor.forClass(Sablona.class);
        verify(sablonaRepository, times(5)).save(zachycene.capture());
        assertThat(zachycene.getAllValues()).extracting(Sablona::getNazev)
                .containsExactly("Smlouva o poskytování služeb", "Cenová nabídka", "Faktura",
                        "Protokol o předání", "Plná moc");
        assertThat(uloziste.existuje("smlouva.docx")).isTrue();
        assertThat(uloziste.existuje("nabidka.docx")).isTrue();
    }

    @Test
    void existujiciZaznamVDatabaziSeNeprepisuje() throws IOException {
        given(sablonaRepository.existsByNazev(any())).willReturn(true);
        given(smazaneVestaveneRepository.existsByNazev(any())).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        verify(sablonaRepository, never()).save(any());
    }

    @Test
    void existujiciSouborNaDiskuSeNeprepiseAniKdyzZaznamVDatabaziChybi() throws IOException {
        uloziste.uloz("smlouva.docx", "uzivatelem upravena verze".getBytes());
        given(sablonaRepository.existsByNazev(any())).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev(any())).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        assertThat(uloziste.nacti("smlouva.docx")).isEqualTo("uzivatelem upravena verze".getBytes());
    }

    @Test
    void tombstonovanaSablonaSeVubecNeobnoviNaDiskuAniVDatabazi() throws IOException {
        given(sablonaRepository.existsByNazev(any())).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev("Smlouva o poskytování služeb")).willReturn(true);
        given(smazaneVestaveneRepository.existsByNazev("Cenová nabídka")).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev("Faktura")).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev("Protokol o předání")).willReturn(false);
        given(smazaneVestaveneRepository.existsByNazev("Plná moc")).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        assertThat(uloziste.existuje("smlouva.docx")).isFalse();
        ArgumentCaptor<Sablona> zachycene = ArgumentCaptor.forClass(Sablona.class);
        verify(sablonaRepository, times(4)).save(zachycene.capture());
        assertThat(zachycene.getAllValues()).extracting(Sablona::getNazev)
                .doesNotContain("Smlouva o poskytování služeb");
    }
}
