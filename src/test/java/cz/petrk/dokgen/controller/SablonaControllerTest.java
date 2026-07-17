package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.SablonaVerze;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.SablonaUlozisteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SablonaController.class)
@WithMockUser
class SablonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentGeneratorService documentGeneratorService;

    @MockitoBean
    private SablonaRepository sablonaRepository;

    @MockitoBean
    private SablonaUlozisteService uloziste;

    @Test
    void seznamZobraziDostupneSablony() throws Exception {
        given(documentGeneratorService.getDostupneSablony())
                .willReturn(List.of(new Sablona("Smlouva", "smlouva.docx", true)));

        mockMvc.perform(get("/sablony"))
                .andExpect(status().isOk())
                .andExpect(view().name("sablony"))
                .andExpect(model().attributeExists("sablony"));
    }

    @Test
    void nahratPlatnouSablonuPresmerujeBezChyby() throws Exception {
        MockMultipartFile soubor = new MockMultipartFile("soubor", "novy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "obsah".getBytes());

        mockMvc.perform(multipart("/sablony/nahrat").file(soubor).param("nazev", "Nová šablona").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attributeCount(0));

        verify(documentGeneratorService).nahrajNovouSablonu(eq("Nová šablona"), any());
    }

    @Test
    void nahratSPrazdnymNazvemVratiChybuVeFlashAtributu() throws Exception {
        MockMultipartFile soubor = new MockMultipartFile("soubor", "novy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "obsah".getBytes());

        mockMvc.perform(multipart("/sablony/nahrat").file(soubor).param("nazev", "").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("chybaNahrani", "Název šablony je povinný."));

        verify(documentGeneratorService, never()).nahrajNovouSablonu(any(), any());
    }

    // Prazdny soubor kontroluje uz samotny controller, jeste pred volanim servisni vrstvy.
    @Test
    void nahratSPrazdnymSouboremVratiChybuVeFlashAtributu() throws Exception {
        MockMultipartFile prazdnySoubor = new MockMultipartFile("soubor", "prazdny.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[0]);

        mockMvc.perform(multipart("/sablony/nahrat").file(prazdnySoubor).param("nazev", "Test").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("chybaNahrani", "Musíš vybrat soubor .docx k nahrání."));

        verify(documentGeneratorService, never()).nahrajNovouSablonu(any(), any());
    }

    @Test
    void nahratSDuplicitnimNazvemVratiChybuVeFlashAtributu() throws Exception {
        MockMultipartFile soubor = new MockMultipartFile("soubor", "novy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "obsah".getBytes());
        given(documentGeneratorService.nahrajNovouSablonu(eq("Duplicitní"), any()))
                .willThrow(new IllegalArgumentException("Šablona s názvem \"Duplicitní\" už existuje."));

        mockMvc.perform(multipart("/sablony/nahrat").file(soubor).param("nazev", "Duplicitní").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("chybaNahrani", "Šablona s názvem \"Duplicitní\" už existuje."));
    }

    @Test
    void stahnoutExistujiciSablonuVratiObsah() throws Exception {
        given(sablonaRepository.findById(1L)).willReturn(Optional.of(new Sablona("Smlouva", "smlouva.docx", true)));
        given(documentGeneratorService.stahniSablonu(1L)).willReturn("obsah souboru".getBytes());

        mockMvc.perform(get("/sablony/1/stahnout"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("obsah souboru".getBytes()))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void stahnoutNeexistujiciSablonuVratiChybovouStranku() throws Exception {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/sablony/999/stahnout"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void nahraditPlatnymSouboremPresmerujeBezChyby() throws Exception {
        MockMultipartFile soubor = new MockMultipartFile("soubor", "upraveno.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "obsah".getBytes());

        mockMvc.perform(multipart("/sablony/1/nahradit").file(soubor).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attributeCount(0));

        verify(documentGeneratorService).nahradSouborSablony(eq(1L), any());
    }

    @Test
    void nahraditNevalidnimSouboremVratiChybuVeFlashAtributu() throws Exception {
        MockMultipartFile soubor = new MockMultipartFile("soubor", "spatny.docx", "text/plain", "neni docx".getBytes());
        willThrow(new IllegalArgumentException("Nahraný soubor není platný Word dokument (.docx)."))
                .given(documentGeneratorService).nahradSouborSablony(eq(1L), any());

        mockMvc.perform(multipart("/sablony/1/nahradit").file(soubor).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("chybaNahrani", "Nahraný soubor není platný Word dokument (.docx)."));
    }

    @Test
    void upravitExistujiciSablonuPresmerujeSInfoHlaskou() throws Exception {
        given(sablonaRepository.findById(1L))
                .willReturn(Optional.of(new Sablona("Smlouva o poskytování služeb", "smlouva.docx", true)));

        mockMvc.perform(post("/sablony/1/upravit").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("otevrenaSablona", "Smlouva o poskytování služeb"));

        verify(uloziste).otevriVeVychoziAplikaci("smlouva.docx");
    }

    @Test
    void upravitNeexistujiciSablonuVratiChybovouStranku() throws Exception {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(post("/sablony/999/upravit").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));

        verify(uloziste, never()).otevriVeVychoziAplikaci(any());
    }

    // Napr. appka bezici na headless serveru bez GUI - SablonaUlozisteService v tom
    // pripade hodi IOException se srozumitelnou zpravou, kterou GlobalExceptionHandler
    // zobrazi na chyba.html misto pádu appky.
    @Test
    void upravitKdyzOtevreniSelzeVratiChybovouStranku() throws Exception {
        given(sablonaRepository.findById(1L)).willReturn(Optional.of(new Sablona("Smlouva", "smlouva.docx", true)));
        willThrow(new IOException("Otevření šablony ve výchozí aplikaci tady není možné."))
                .given(uloziste).otevriVeVychoziAplikaci("smlouva.docx");

        mockMvc.perform(post("/sablony/1/upravit").with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("chyba"));
    }

    @Test
    void smazatExistujiciSablonuPresmerujeBezChyby() throws Exception {
        mockMvc.perform(post("/sablony/smazat/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attributeCount(0));

        verify(documentGeneratorService).smazSablonu(1L);
    }

    @Test
    void smazatNeexistujiciSablonuVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Šablona s id 999 neexistuje"))
                .given(documentGeneratorService).smazSablonu(999L);

        mockMvc.perform(post("/sablony/smazat/999").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony"))
                .andExpect(flash().attribute("chybaNahrani", "Šablona s id 999 neexistuje"));
    }

    @Test
    void verzeZobraziSeznamStarsichVerzi() throws Exception {
        given(sablonaRepository.findById(1L)).willReturn(Optional.of(new Sablona("Smlouva", "smlouva.docx", true)));
        given(documentGeneratorService.getVerze(1L)).willReturn(List.of(new SablonaVerze(1L, "verze-a.docx")));

        mockMvc.perform(get("/sablony/1/verze"))
                .andExpect(status().isOk())
                .andExpect(view().name("verze"))
                .andExpect(model().attributeExists("sablona", "verze"));
    }

    @Test
    void verzeNeexistujiciSablonyVratiChybovouStranku() throws Exception {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/sablony/999/verze"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void stahnoutVerziVratiObsah() throws Exception {
        given(sablonaRepository.findById(1L)).willReturn(Optional.of(new Sablona("Smlouva", "smlouva.docx", true)));
        given(documentGeneratorService.stahniVerzi(1L, 7L)).willReturn("stara verze".getBytes());

        mockMvc.perform(get("/sablony/1/verze/7/stahnout"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("stara verze".getBytes()))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    void stahnoutVerziNeznameSablonyVratiChybovouStranku() throws Exception {
        given(sablonaRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/sablony/999/verze/7/stahnout"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }

    @Test
    void obnovitVerziPresmerujeZpetNaVerzeSPotvrzenim() throws Exception {
        mockMvc.perform(post("/sablony/1/verze/7/obnovit").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sablony/1/verze"))
                .andExpect(flash().attribute("verzeObnovena", true));

        verify(documentGeneratorService).obnovVerzi(1L, 7L);
    }

    @Test
    void obnovitNeexistujiciVerziVyhodiChybu() throws Exception {
        willThrow(new IllegalArgumentException("Verze šablony (id 7) neexistuje nebo nepatří k této šabloně."))
                .given(documentGeneratorService).obnovVerzi(1L, 7L);

        mockMvc.perform(post("/sablony/1/verze/7/obnovit").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("chyba"));
    }
}
