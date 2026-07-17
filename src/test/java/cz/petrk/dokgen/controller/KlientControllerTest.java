package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.KlientRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.HistorieService;
import cz.petrk.dokgen.service.PdfExportService;
import cz.petrk.dokgen.service.VygenerovanyDokumentUlozisteService;
import cz.petrk.dokgen.service.VysledekGenerovani;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KlientController.class)
@WithMockUser
class KlientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KlientRepository klientRepository;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private PdfExportService pdfExportService;

    @MockBean
    private HistorieService historieService;

    @MockBean
    private VygenerovanyDokumentUlozisteService vygenerovanyDokumentUloziste;

    @Test
    void seznamZobraziVsechnyKlienty() throws Exception {
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(new Klient()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("seznam"))
                .andExpect(model().attributeExists("klienti"));
    }

    // Prazdny retezec (jak ho posle formular u nevyplneneho pole) neni v Thymeleaf
    // th:if pravdivostne stejny jako null - musi se osetrit #strings.isEmpty(),
    // jinak by se vykreslil prazdny <a href="tel:"> misto hlasky "neuvedeno".
    @Test
    void seznamZobrazujeNeuvedenoProPrazdnyTelefonAEmail() throws Exception {
        Klient bezKontaktu = vzorovyKlient(1L);
        bezKontaktu.setTelefon("");
        bezKontaktu.setEmail("");
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(bezKontaktu));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("neuvedeno")))
                .andExpect(content().string(not(containsString("href=\"tel:\""))))
                .andExpect(content().string(not(containsString("href=\"mailto:\""))));
    }

    @Test
    void seznamSHledanimVratiJenOdpovidajiciKlienty() throws Exception {
        Klient novak = vzorovyKlient(1L);
        Klient svoboda = new Klient();
        svoboda.setId(2L);
        svoboda.setJmeno("Petr");
        svoboda.setPrijmeni("Svoboda");
        svoboda.setTelefon("777888999");
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(novak, svoboda));

        mockMvc.perform(get("/").param("hledat", "novák"))
                .andExpect(status().isOk())
                .andExpect(view().name("seznam"))
                .andExpect(model().attribute("klienti", List.of(novak)));
    }

    @Test
    void seznamSHledanimHledaIVTelefonuAEmailu() throws Exception {
        Klient novak = vzorovyKlient(1L);
        novak.setTelefon("777123456");
        novak.setEmail("jan.novak@example.cz");
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(novak));

        mockMvc.perform(get("/").param("hledat", "777123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("klienti", List.of(novak)));

        mockMvc.perform(get("/").param("hledat", "example.cz"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("klienti", List.of(novak)));
    }

    @Test
    void seznamSHledanimBezShodyVratiPrazdnySeznam() throws Exception {
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(vzorovyKlient(1L)));

        mockMvc.perform(get("/").param("hledat", "neexistujici-jmeno"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("klienti", List.of()));
    }

    @Test
    void seznamStrankujePri21Klientech() throws Exception {
        List<Klient> klienti = new java.util.ArrayList<>();
        for (long i = 1; i <= 21; i++) {
            klienti.add(vzorovyKlient(i));
        }
        given(klientRepository.findAll(any(Sort.class))).willReturn(klienti);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("celkemStranek", 2))
                .andExpect(model().attribute("strana", 0))
                .andExpect(model().attribute("klienti", klienti.subList(0, 20)));

        mockMvc.perform(get("/").param("strana", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("strana", 1))
                .andExpect(model().attribute("klienti", klienti.subList(20, 21)));
    }

    @Test
    void seznamSPrilisVysokymCislemStrankyVratiPosledniStranku() throws Exception {
        given(klientRepository.findAll(any(Sort.class))).willReturn(List.of(vzorovyKlient(1L)));

        mockMvc.perform(get("/").param("strana", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("strana", 0))
                .andExpect(model().attribute("celkemStranek", 1));
    }

    @Test
    void ulozitPlatnehoKlientaPresmerujeNaSeznam() throws Exception {
        mockMvc.perform(post("/ulozit")
                        .with(csrf())
                        .param("jmeno", "Jan")
                        .param("prijmeni", "Novák")
                        .param("email", "jan@example.cz")
                        .param("psc", "11000")
                        .param("ico", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(klientRepository).save(any(Klient.class));
    }

    @Test
    void ulozitKlientaBezJmenaVratiFormularSChybou() throws Exception {
        mockMvc.perform(post("/ulozit")
                        .with(csrf())
                        .param("jmeno", "")
                        .param("prijmeni", "Novák"))
                .andExpect(status().isOk())
                .andExpect(view().name("formular"))
                .andExpect(model().attributeHasFieldErrors("klient", "jmeno"));

        verify(klientRepository, never()).save(any(Klient.class));
    }

    @Test
    void ulozitKlientaSNeplatnymEmailemVratiFormularSChybou() throws Exception {
        mockMvc.perform(post("/ulozit")
                        .with(csrf())
                        .param("jmeno", "Jan")
                        .param("prijmeni", "Novák")
                        .param("email", "neplatny-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("formular"))
                .andExpect(model().attributeHasFieldErrors("klient", "email"));

        verify(klientRepository, never()).save(any(Klient.class));
    }

    @Test
    void upravitNeexistujicihoKlientaVratiChybovouStranku() throws Exception {
        given(klientRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/upravit/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    // Edge case: retezec presahujici @Size(max = 100) na jmenu.
    @Test
    void ulozitKlientaSPrilisDlouhymJmenemVratiFormularSChybou() throws Exception {
        String prilisDlouheJmeno = "A".repeat(101);

        mockMvc.perform(post("/ulozit")
                        .with(csrf())
                        .param("jmeno", prilisDlouheJmeno)
                        .param("prijmeni", "Novák"))
                .andExpect(status().isOk())
                .andExpect(view().name("formular"))
                .andExpect(model().attributeHasFieldErrors("klient", "jmeno"));

        verify(klientRepository, never()).save(any(Klient.class));
    }

    @Test
    void smazatExistujicihoKlientaPresmerujeNaSeznam() throws Exception {
        given(klientRepository.existsById(1L)).willReturn(true);

        mockMvc.perform(post("/smazat/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(klientRepository).deleteById(1L);
    }

    @Test
    void smazatNeexistujicihoKlientaVratiChybovouStranku() throws Exception {
        given(klientRepository.existsById(999L)).willReturn(false);

        mockMvc.perform(post("/smazat/999").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));

        verify(klientRepository, never()).deleteById(any());
    }

    @Test
    void vyberSablonyZobraziFormularProExistujicihoKlienta() throws Exception {
        Klient klient = new Klient();
        klient.setId(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        given(documentGeneratorService.getDostupneSablony()).willReturn(List.of());

        mockMvc.perform(get("/generovat/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("generovat"))
                .andExpect(model().attributeExists("klient", "sablony"));
    }

    @Test
    void vyberSablonyProNeexistujicihoKlientaVratiChybovouStranku() throws Exception {
        given(klientRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/generovat/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void generujDokumentVratiWordSoubor() throws Exception {
        Klient klient = vzorovyKlient(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        Sablona sablona = new Sablona("Smlouva", "smlouva.docx", true);
        byte[] obsah = "obsah dokumentu".getBytes();
        given(documentGeneratorService.vygenerujDokument(any(Klient.class), eq(1L)))
                .willReturn(new VysledekGenerovani(obsah, sablona));
        VygenerovanyDokument zaznam = new VygenerovanyDokument(1L, "Jan Novák", "Smlouva", "WORD");
        ReflectionTestUtils.setField(zaznam, "id", 42L);
        given(historieService.zaznamenej(klient, sablona, "WORD")).willReturn(zaznam);

        mockMvc.perform(post("/generovat/1").with(csrf())
                        .param("sablonaId", "1")
                        .param("format", "WORD"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(obsah))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));

        verify(historieService).zaznamenej(klient, sablona, "WORD");
        verify(vygenerovanyDokumentUloziste).uloz(42L, "WORD", obsah);
    }

    @Test
    void generujDokumentVratiPdfSoubor() throws Exception {
        Klient klient = vzorovyKlient(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        Sablona sablona = new Sablona("Smlouva", "smlouva.docx", true);
        byte[] wordObsah = "word".getBytes();
        byte[] pdfObsah = "pdf".getBytes();
        given(documentGeneratorService.vygenerujDokument(any(Klient.class), eq(1L)))
                .willReturn(new VysledekGenerovani(wordObsah, sablona));
        given(pdfExportService.prevedNaPdf(wordObsah)).willReturn(pdfObsah);
        VygenerovanyDokument zaznam = new VygenerovanyDokument(1L, "Jan Novák", "Smlouva", "PDF");
        ReflectionTestUtils.setField(zaznam, "id", 43L);
        given(historieService.zaznamenej(klient, sablona, "PDF")).willReturn(zaznam);

        mockMvc.perform(post("/generovat/1").with(csrf())
                        .param("sablonaId", "1")
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfObsah))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

        verify(historieService).zaznamenej(klient, sablona, "PDF");
        verify(vygenerovanyDokumentUloziste).uloz(43L, "PDF", pdfObsah);
    }

    @Test
    void generujDokumentProNeexistujicihoKlientaVratiChybovouStranku() throws Exception {
        given(klientRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(post("/generovat/999").with(csrf())
                        .param("sablonaId", "1")
                        .param("format", "WORD"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void generujDokumentSNeplatnymFormatemVratiChybovouStranku() throws Exception {
        mockMvc.perform(post("/generovat/1").with(csrf())
                        .param("sablonaId", "1")
                        .param("format", "EXE"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("chyba"));

        verify(documentGeneratorService, never()).vygenerujDokument(any(), any());
    }

    @Test
    void generujDokumentSNeznamouSablonouVratiChybovouStranku() throws Exception {
        Klient klient = vzorovyKlient(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        given(documentGeneratorService.vygenerujDokument(any(Klient.class), eq(999L)))
                .willThrow(new IllegalArgumentException("Neznámá šablona (id 999)"));

        mockMvc.perform(post("/generovat/1").with(csrf())
                        .param("sablonaId", "999")
                        .param("format", "WORD"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void nahledDokumentuVratiPdfInline() throws Exception {
        Klient klient = vzorovyKlient(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        Sablona sablona = new Sablona("Smlouva", "smlouva.docx", true);
        byte[] wordObsah = "word".getBytes();
        byte[] pdfObsah = "pdf".getBytes();
        given(documentGeneratorService.vygenerujDokument(any(Klient.class), eq(1L)))
                .willReturn(new VysledekGenerovani(wordObsah, sablona));
        given(pdfExportService.prevedNaPdf(wordObsah)).willReturn(pdfObsah);

        mockMvc.perform(get("/generovat/1/nahled").param("sablonaId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfObsah))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("inline")));

        verify(historieService, never()).zaznamenej(any(), any(), any());
        verify(vygenerovanyDokumentUloziste, never()).uloz(any(), any(), any());
    }

    @Test
    void nahledDokumentuProNeexistujicihoKlientaVratiChybovouStranku() throws Exception {
        given(klientRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/generovat/999/nahled").param("sablonaId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void nahledDokumentuSNeznamouSablonouVratiChybovouStranku() throws Exception {
        Klient klient = vzorovyKlient(1L);
        given(klientRepository.findById(1L)).willReturn(Optional.of(klient));
        given(documentGeneratorService.vygenerujDokument(any(Klient.class), eq(999L)))
                .willThrow(new IllegalArgumentException("Neznámá šablona (id 999)"));

        mockMvc.perform(get("/generovat/1/nahled").param("sablonaId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    private Klient vzorovyKlient(Long id) {
        Klient klient = new Klient();
        klient.setId(id);
        klient.setJmeno("Jan");
        klient.setPrijmeni("Novák");
        return klient;
    }
}
