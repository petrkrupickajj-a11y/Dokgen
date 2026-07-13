package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.SablonaVerze;
import cz.petrk.dokgen.entity.SmazanaVestavenaSablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.repository.SablonaVerzeRepository;
import cz.petrk.dokgen.repository.SmazanaVestavenaSablonaRepository;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DocumentGeneratorServiceTest {

    @TempDir
    Path uloznyAdresar;

    private SablonaRepository sablonaRepository;
    private SablonaUlozisteService uloziste;
    private SmazanaVestavenaSablonaRepository smazaneVestaveneRepository;
    private SablonaVerzeRepository sablonaVerzeRepository;
    private DocumentGeneratorService service;

    @BeforeEach
    void setUp() throws IOException {
        // stejne nastaveni jako PoiBezpecnostConfig v bezici appce - v cistem
        // unit testu (bez Spring kontextu) se @PostConstruct nespusti sam
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(100L * 1024 * 1024);

        // Ciste unit testy bez Spring MVC pozadavku nemaji zadny LocaleResolver,
        // ktery by nastavil jazyk - bez tohohle by test byl zavisly na jazyce
        // stroje, na kterem bezi (LocaleContextHolder by spadl na Locale.getDefault()).
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        sablonaRepository = Mockito.mock(SablonaRepository.class);
        uloziste = new SablonaUlozisteService(uloznyAdresar.toString(), zpravy);
        smazaneVestaveneRepository = Mockito.mock(SmazanaVestavenaSablonaRepository.class);
        sablonaVerzeRepository = Mockito.mock(SablonaVerzeRepository.class);
        given(sablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc(any())).willReturn(new ArrayList<>());
        service = new DocumentGeneratorService(sablonaRepository, uloziste, smazaneVestaveneRepository,
                sablonaVerzeRepository, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private void pripravSablonu(long id, String nazev, String souborNazev) throws IOException {
        try (var vstup = new ClassPathResource("word-templates/" + souborNazev).getInputStream()) {
            uloziste.uloz(souborNazev, vstup.readAllBytes());
        }
        given(sablonaRepository.findById(id)).willReturn(Optional.of(new Sablona(nazev, souborNazev, true)));
    }

    private Klient vzorovyKlient() {
        Klient klient = new Klient();
        klient.setJmeno("Jan");
        klient.setPrijmeni("Novák");
        klient.setTelefon("777123456");
        klient.setEmail("jan.novak@example.cz");
        klient.setAdresa("Hlavní 1");
        klient.setMesto("Praha");
        klient.setPsc("11000");
        klient.setIco("12345678");
        klient.setPoznamka("VIP klient");
        return klient;
    }

    @Test
    void vygenerujDokumentNahradiPlaceholderyDatyKlienta() throws IOException {
        pripravSablonu(1L, "Smlouva o poskytování služeb", "smlouva.docx");

        byte[] dokument = service.vygenerujDokument(vzorovyKlient(), 1L).obsah();

        String text = celyTextDokumentu(dokument);
        assertThat(text).contains("Jan");
        assertThat(text).contains("Novák");
        assertThat(text).doesNotContain("${jmeno}");
        assertThat(text).doesNotContain("${prijmeni}");
    }

    @Test
    void vygenerujDokumentSNullHodnotamiNevyhodiChybu() throws IOException {
        pripravSablonu(2L, "Cenová nabídka", "nabidka.docx");

        Klient klient = new Klient();
        klient.setJmeno("Eva");
        klient.setPrijmeni("Malá");
        // ostatni pole zustavaji null

        byte[] dokument = service.vygenerujDokument(klient, 2L).obsah();

        String text = celyTextDokumentu(dokument);
        assertThat(text).doesNotContain("${");
    }

    @ParameterizedTest
    @CsvSource({
            "1, 'Smlouva o poskytování služeb', smlouva.docx",
            "2, 'Cenová nabídka', nabidka.docx",
            "3, Faktura, faktura.docx",
            "4, 'Protokol o předání', protokol.docx",
            "5, 'Plná moc', plna_moc.docx"
    })
    void kazdaSablonaSeVygenerujeBezZbylychPlaceholderu(long id, String nazev, String soubor) throws IOException {
        pripravSablonu(id, nazev, soubor);

        byte[] dokument = service.vygenerujDokument(vzorovyKlient(), id).obsah();

        String text = celyTextDokumentu(dokument);
        assertThat(text).doesNotContain("${");
        assertThat(text).contains("Novák");
    }

    @Test
    void vygenerujDokumentNerozpoznaPlaceholderSkryteVzniklyUvnitrHodnotyJinehoPole() throws IOException {
        // Sablona ma dve pole vedle sebe - poznamku klienta a datum vygenerovani
        uloziste.uloz("skryty-placeholder.docx", docxSTextem("Poznámka: ${poznamka} Datum: ${datum}"));
        given(sablonaRepository.findById(60L)).willReturn(Optional.of(
                new Sablona("Test - hodnota vypadající jako jiný placeholder", "skryty-placeholder.docx", false)));

        Klient klient = vzorovyKlient();
        // Hodnota poznamky nahodou obsahuje retezec "${datum}" jako obycejny text
        klient.setPoznamka("Platba proběhla dne ${datum} v hotovosti.");

        byte[] dokument = service.vygenerujDokument(klient, 60L).obsah();
        String text = celyTextDokumentu(dokument);

        // "${datum}" v poznamce je jen doslovny text klienta, ne skutecny placeholder
        // ze sablony - nesmi se dodatecne prepsat dnesnim datem
        assertThat(text).contains("Platba proběhla dne ${datum} v hotovosti.");
    }

    @Test
    void vygenerujDokumentNahradiPlaceholderyVZapatiIVeVnorenTabulce() throws IOException {
        uloziste.uloz("zapati-vnorena-tabulka.docx", docxSZapatimAVnorenouTabulkou());
        given(sablonaRepository.findById(70L)).willReturn(Optional.of(
                new Sablona("Test - zápatí a vnořená tabulka", "zapati-vnorena-tabulka.docx", false)));

        byte[] dokument = service.vygenerujDokument(vzorovyKlient(), 70L).obsah();

        try (XWPFDocument vysledek = new XWPFDocument(new ByteArrayInputStream(dokument))) {
            String textZapati = vysledek.getFooterList().get(0).getText();
            assertThat(textZapati).contains("Jan").doesNotContain("${jmeno}");

            String textVnoreneTabulky = vysledek.getTables().get(0).getRow(0).getCell(0)
                    .getTables().get(0).getRow(0).getCell(0).getText();
            assertThat(textVnoreneTabulky).contains("Novák").doesNotContain("${prijmeni}");
        }
    }

    /** Sablona s placeholderem v zapati a v tabulce vnorene uvnitr bunky jine tabulky. */
    private byte[] docxSZapatimAVnorenouTabulkou() throws IOException {
        try (XWPFDocument dokument = new XWPFDocument()) {
            XWPFTable vnejsiTabulka = dokument.createTable(1, 1);
            XWPFTableCell vnejsiBunka = vnejsiTabulka.getRow(0).getCell(0);

            XmlCursor kurzor = vnejsiBunka.getParagraphs().get(0).getCTP().newCursor();
            XWPFTable vnorenaTabulka = vnejsiBunka.insertNewTbl(kurzor);
            kurzor.dispose();
            // insertNewTbl vytvori jen prazdnou tabulku bez radku - radek a bunku je potreba doplnit rucne
            XWPFTableRow vnorenyRadek = vnorenaTabulka.createRow();
            XWPFTableCell vnorenaBunka = vnorenyRadek.getCell(0) != null ? vnorenyRadek.getCell(0) : vnorenyRadek.createCell();
            vnorenaBunka.setText("${prijmeni}");

            XWPFHeaderFooterPolicy politika = dokument.createHeaderFooterPolicy();
            XWPFFooter zapati = politika.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            zapati.createParagraph().createRun().setText("${jmeno}");

            ByteArrayOutputStream vystup = new ByteArrayOutputStream();
            dokument.write(vystup);
            return vystup.toByteArray();
        }
    }

    @Test
    void getDostupneSablonyVraciSablonyZRepositoryeSerazeneDlePodleNazvu() {
        List<Sablona> ocekavane = List.of(new Sablona("Faktura", "faktura.docx", true));
        given(sablonaRepository.findAll(any(Sort.class))).willReturn(ocekavane);

        assertThat(service.getDostupneSablony()).isEqualTo(ocekavane);
    }

    @Test
    void neznamaSablonaVyhodiIllegalArgumentException() {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        Klient klient = new Klient();
        klient.setJmeno("Petr");
        klient.setPrijmeni("Svoboda");

        assertThatThrownBy(() -> service.vygenerujDokument(klient, 999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chybejiciSouborSablonyVyhodiIOExceptionSPratelskouZpravou() {
        given(sablonaRepository.findById(42L))
                .willReturn(Optional.of(new Sablona("Test - chybějící soubor", "neexistuje-vubec.docx", false)));

        Klient klient = new Klient();
        klient.setJmeno("Petr");
        klient.setPrijmeni("Svoboda");

        assertThatThrownBy(() -> service.vygenerujDokument(klient, 42L))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("chybí nebo je poškozený");
    }

    @Test
    void poskozenySouborSablonyVyhodiIOExceptionSPratelskouZpravou() throws IOException {
        uloziste.uloz("poskozena.docx", "Toto neni platny .docx soubor".getBytes());
        given(sablonaRepository.findById(43L))
                .willReturn(Optional.of(new Sablona("Test - poškozený soubor", "poskozena.docx", false)));

        Klient klient = new Klient();
        klient.setJmeno("Petr");
        klient.setPrijmeni("Svoboda");

        assertThatThrownBy(() -> service.vygenerujDokument(klient, 43L))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("poškozená");
    }

    @Test
    void nahrajNovouSablonuUloziZaznamISoubor() throws IOException {
        given(sablonaRepository.existsByNazev("Nová šablona")).willReturn(false);
        given(sablonaRepository.save(any(Sablona.class))).willAnswer(vyvolani -> vyvolani.getArgument(0));

        byte[] platnyDocx = nactiPlatnyDocx();
        MockMultipartFile soubor = new MockMultipartFile("soubor", "novy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", platnyDocx);

        Sablona ulozena = service.nahrajNovouSablonu("Nová šablona", soubor);

        assertThat(ulozena.getNazev()).isEqualTo("Nová šablona");
        assertThat(ulozena.isVestavena()).isFalse();
    }

    @Test
    void nahrajNovouSablonuSDuplicitnimNazvemVyhodiChybu() {
        given(sablonaRepository.existsByNazev("Duplicitní")).willReturn(true);
        MockMultipartFile soubor = new MockMultipartFile("soubor", "x.docx", "application/octet-stream",
                new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.nahrajNovouSablonu("Duplicitní", soubor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nahrajNovouSablonuSPodezrelymZipemVyhodiChybuOZipBombe() throws IOException {
        given(sablonaRepository.existsByNazev(any())).willReturn(false);

        byte[] podezrelySoubor = vytvorZipSPodezrelymPomeremKomprese();
        MockMultipartFile soubor = new MockMultipartFile("soubor", "podezrely.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", podezrelySoubor);

        assertThatThrownBy(() -> service.nahrajNovouSablonu("Podezřelá šablona", soubor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip bombu");
    }

    @Test
    void nahrajNovouSablonuSNevalidnimSouboremVyhodiChybu() {
        given(sablonaRepository.existsByNazev(any())).willReturn(false);
        MockMultipartFile soubor = new MockMultipartFile("soubor", "x.docx", "application/octet-stream",
                "toto neni docx".getBytes());

        assertThatThrownBy(() -> service.nahrajNovouSablonu("Špatný soubor", soubor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("není platný Word dokument");
    }

    @Test
    void stahniSablonuVratiAktualniObsahSouboru() throws IOException {
        pripravSablonu(7L, "Smlouva o poskytování služeb", "smlouva.docx");

        byte[] stazeno = service.stahniSablonu(7L);

        assertThat(stazeno).isNotEmpty();
        String text = celyTextDokumentu(stazeno);
        assertThat(text).contains("${jmeno}");
    }

    @Test
    void stahniSablonuNeexistujiciVyhodiChybu() {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.stahniSablonu(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nahradSouborSablonyPrepiseObsahAOznaciUpraveno() throws IOException {
        pripravSablonu(8L, "Cenová nabídka", "nabidka.docx");
        Sablona sablona = sablonaRepository.findById(8L).orElseThrow();
        java.time.LocalDateTime puvodniCas = sablona.getNahranoDne();

        byte[] novyObsah = nactiPlatnyDocx();
        MockMultipartFile soubor = new MockMultipartFile("soubor", "upraveno.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", novyObsah);
        given(sablonaRepository.save(any(Sablona.class))).willAnswer(vyvolani -> vyvolani.getArgument(0));

        service.nahradSouborSablony(8L, soubor);

        assertThat(uloziste.nacti("nabidka.docx")).isEqualTo(novyObsah);
        assertThat(sablona.getNahranoDne()).isAfterOrEqualTo(puvodniCas);
    }

    @Test
    void nahradSouborSablonySNevalidnimSouboremVyhodiChybu() throws IOException {
        pripravSablonu(9L, "Faktura", "faktura.docx");
        MockMultipartFile soubor = new MockMultipartFile("soubor", "spatny.docx", "application/octet-stream",
                "toto neni docx".getBytes());

        assertThatThrownBy(() -> service.nahradSouborSablony(9L, soubor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("není platný Word dokument");
    }

    @Test
    void nahradSouborSablonyUlozíPredchoziObsahJakoVerzi() throws IOException {
        pripravSablonu(10L, "Cenová nabídka", "nabidka.docx");
        Sablona sablona = sablonaRepository.findById(10L).orElseThrow();
        ReflectionTestUtils.setField(sablona, "id", 10L);
        byte[] puvodniObsah = uloziste.nacti("nabidka.docx");
        given(sablonaRepository.save(any(Sablona.class))).willAnswer(vyvolani -> vyvolani.getArgument(0));

        MockMultipartFile soubor = new MockMultipartFile("soubor", "novy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", nactiPlatnyDocx());
        service.nahradSouborSablony(10L, soubor);

        ArgumentCaptor<SablonaVerze> zachycena = ArgumentCaptor.forClass(SablonaVerze.class);
        verify(sablonaVerzeRepository).save(zachycena.capture());
        assertThat(zachycena.getValue().getSablonaId()).isEqualTo(10L);
        assertThat(uloziste.nacti(zachycena.getValue().getNazevSouboru())).isEqualTo(puvodniObsah);
    }

    @Test
    void getVerzeVraciVerzeZRepositoryeProDanouSablonu() throws IOException {
        pripravSablonu(11L, "Faktura", "faktura.docx");
        given(sablonaRepository.existsById(11L)).willReturn(true);
        List<SablonaVerze> ocekavane = List.of(new SablonaVerze(11L, "verze-1.docx"));
        given(sablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc(11L)).willReturn(ocekavane);

        assertThat(service.getVerze(11L)).isEqualTo(ocekavane);
    }

    @Test
    void getVerzeNeexistujiciSablonaVyhodiChybu() {
        given(sablonaRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.getVerze(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stahniVerziVratiObsahUlozenePredchoziVerze() throws IOException {
        byte[] obsahVerze = nactiPlatnyDocx();
        uloziste.uloz("verze-stara.docx", obsahVerze);
        SablonaVerze verze = new SablonaVerze(12L, "verze-stara.docx");
        ReflectionTestUtils.setField(verze, "id", 5L);
        given(sablonaVerzeRepository.findById(5L)).willReturn(Optional.of(verze));

        assertThat(service.stahniVerzi(12L, 5L)).isEqualTo(obsahVerze);
    }

    @Test
    void stahniVerziPatricyJineSablonSVyhodiChybu() throws IOException {
        uloziste.uloz("verze-stara.docx", nactiPlatnyDocx());
        SablonaVerze verze = new SablonaVerze(12L, "verze-stara.docx");
        ReflectionTestUtils.setField(verze, "id", 5L);
        given(sablonaVerzeRepository.findById(5L)).willReturn(Optional.of(verze));

        assertThatThrownBy(() -> service.stahniVerzi(999L, 5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obnovVerziObnoviObsahAUlozíAktualniJakoNovouVerzi() throws IOException {
        pripravSablonu(13L, "Protokol o předání", "protokol.docx");
        Sablona sablona = sablonaRepository.findById(13L).orElseThrow();
        ReflectionTestUtils.setField(sablona, "id", 13L);
        given(sablonaRepository.save(any(Sablona.class))).willAnswer(vyvolani -> vyvolani.getArgument(0));

        byte[] puvodniAktualniObsah = uloziste.nacti("protokol.docx");
        byte[] obsahStareVerze = nactiPlatnyDocx();
        uloziste.uloz("verze-stara.docx", obsahStareVerze);
        SablonaVerze verze = new SablonaVerze(13L, "verze-stara.docx");
        ReflectionTestUtils.setField(verze, "id", 6L);
        given(sablonaVerzeRepository.findById(6L)).willReturn(Optional.of(verze));

        service.obnovVerzi(13L, 6L);

        assertThat(uloziste.nacti("protokol.docx")).isEqualTo(obsahStareVerze);

        ArgumentCaptor<SablonaVerze> zachycena = ArgumentCaptor.forClass(SablonaVerze.class);
        verify(sablonaVerzeRepository).save(zachycena.capture());
        assertThat(uloziste.nacti(zachycena.getValue().getNazevSouboru())).isEqualTo(puvodniAktualniObsah);
    }

    @Test
    void obnovVerziNeexistujiciVerzeVyhodiChybu() throws IOException {
        pripravSablonu(14L, "Plná moc", "plna_moc.docx");
        given(sablonaVerzeRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.obnovVerzi(14L, 404L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void smazSablonuUklidiIStarsiVerze() throws IOException {
        pripravSablonu(15L, "Smlouva o poskytování služeb", "smlouva.docx");
        uloziste.uloz("verze-15-a.docx", nactiPlatnyDocx());
        uloziste.uloz("verze-15-b.docx", nactiPlatnyDocx());
        SablonaVerze verzeA = new SablonaVerze(15L, "verze-15-a.docx");
        SablonaVerze verzeB = new SablonaVerze(15L, "verze-15-b.docx");
        List<SablonaVerze> verze = List.of(verzeA, verzeB);
        given(sablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc(15L)).willReturn(verze);

        service.smazSablonu(15L);

        verify(sablonaVerzeRepository).deleteAll(verze);
        assertThatThrownBy(() -> uloziste.nacti("verze-15-a.docx")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> uloziste.nacti("verze-15-b.docx")).isInstanceOf(IOException.class);
    }

    @Test
    void smazSablonuFungujeIProVestavenouSablonu() throws IOException {
        pripravSablonu(1L, "Smlouva o poskytování služeb", "smlouva.docx");
        Sablona sablona = sablonaRepository.findById(1L).orElseThrow();

        service.smazSablonu(1L);

        verify(sablonaRepository).delete(sablona);
        assertThatThrownBy(() -> uloziste.nacti("smlouva.docx")).isInstanceOf(IOException.class);
    }

    @Test
    void smazSablonuVestavenouZaznamenaTombstoneAbySeNeobnovila() throws IOException {
        pripravSablonu(1L, "Smlouva o poskytování služeb", "smlouva.docx");

        service.smazSablonu(1L);

        ArgumentCaptor<SmazanaVestavenaSablona> zachyceny = ArgumentCaptor.forClass(SmazanaVestavenaSablona.class);
        verify(smazaneVestaveneRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getNazev()).isEqualTo("Smlouva o poskytování služeb");
    }

    @Test
    void smazSablonuVlastniNezaznamenaTombstone() throws IOException {
        given(sablonaRepository.existsByNazev("Vlastní šablona")).willReturn(false);
        given(sablonaRepository.save(any(Sablona.class))).willAnswer(vyvolani -> vyvolani.getArgument(0));
        MockMultipartFile soubor = new MockMultipartFile("soubor", "vlastni.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", nactiPlatnyDocx());
        Sablona ulozena = service.nahrajNovouSablonu("Vlastní šablona", soubor);
        given(sablonaRepository.findById(99L)).willReturn(Optional.of(ulozena));

        service.smazSablonu(99L);

        verify(smazaneVestaveneRepository, never()).save(any());
    }

    @Test
    void smazSablonuNeexistujiciVyhodiChybu() {
        given(sablonaRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.smazSablonu(404L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Vezme skutecny .docx a nahradi obsah "word/document.xml" nekolika
     * miliony opakujicich se znaku - po komprese na max. urovni z toho
     * vznikne archiv s extremne nizkym pomerem komprese (mnohem pod
     * ZipSecureFile.setMinInflateRatio), presne to, co ma nase ochrana
     * proti zip bombe odhalit a odmitnout.
     */
    private byte[] vytvorZipSPodezrelymPomeremKomprese() throws IOException {
        byte[] podezrelyObsah = "A".repeat(20_000_000).getBytes();

        ByteArrayOutputStream vystup = new ByteArrayOutputStream();
        try (ZipInputStream vstup = new ZipInputStream(
                new ClassPathResource("word-templates/smlouva.docx").getInputStream());
             ZipOutputStream zip = new ZipOutputStream(vystup)) {
            zip.setLevel(9);

            ZipEntry polozka;
            while ((polozka = vstup.getNextEntry()) != null) {
                zip.putNextEntry(new ZipEntry(polozka.getName()));
                if ("word/document.xml".equals(polozka.getName())) {
                    zip.write(podezrelyObsah);
                } else {
                    vstup.transferTo(zip);
                }
                zip.closeEntry();
            }
        }
        return vystup.toByteArray();
    }

    private byte[] nactiPlatnyDocx() throws IOException {
        try (var vstup = new ClassPathResource("word-templates/smlouva.docx").getInputStream()) {
            return vstup.readAllBytes();
        }
    }

    /** Vyrobi minimalni .docx o jednom odstavci se zadanym textem - pro testy, ktere nepotrebuji skutecnou sablonu. */
    private byte[] docxSTextem(String text) throws IOException {
        try (XWPFDocument dokument = new XWPFDocument()) {
            XWPFRun run = dokument.createParagraph().createRun();
            run.setText(text);
            ByteArrayOutputStream vystup = new ByteArrayOutputStream();
            dokument.write(vystup);
            return vystup.toByteArray();
        }
    }

    private String celyTextDokumentu(byte[] dokument) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(dokument))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph odstavec : doc.getParagraphs()) {
                sb.append(odstavec.getText()).append('\n');
            }
            doc.getTables().forEach(tabulka -> tabulka.getRows().forEach(radek ->
                    radek.getTableCells().forEach(bunka -> sb.append(bunka.getText()).append('\n'))));
            return sb.toString();
        }
    }
}
