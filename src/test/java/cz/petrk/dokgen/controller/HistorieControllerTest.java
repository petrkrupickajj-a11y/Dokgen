package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import cz.petrk.dokgen.service.HistorieService;
import cz.petrk.dokgen.service.VygenerovanyDokumentUlozisteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HistorieController.class)
@WithMockUser
class HistorieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistorieService historieService;

    @MockitoBean
    private VygenerovanyDokumentRepository vygenerovanyDokumentRepository;

    @MockitoBean
    private VygenerovanyDokumentUlozisteService uloziste;

    private VygenerovanyDokument zaznam(Long id, String format) {
        VygenerovanyDokument zaznam = new VygenerovanyDokument(1L, "Jan Novák", "Smlouva", format);
        ReflectionTestUtils.setField(zaznam, "id", id);
        return zaznam;
    }

    @Test
    void historieBezFiltruZobraziVsechnyZaznamy() throws Exception {
        given(historieService.vyhledej(null, null, null, 0)).willReturn(List.of());
        given(historieService.celkemStranek(null, null, null)).willReturn(1);
        given(historieService.celkemZaznamu(null, null, null)).willReturn(0);

        mockMvc.perform(get("/historie"))
                .andExpect(status().isOk())
                .andExpect(view().name("historie"))
                .andExpect(model().attributeExists("zaznamy", "celkemStranek", "celkemZaznamu", "mesice", "aktualniRok"));
    }

    @Test
    void historieSFiltremPredaFiltrDoSluzby() throws Exception {
        given(historieService.vyhledej(2026, 7, "Novák", 0)).willReturn(List.of());
        given(historieService.celkemStranek(2026, 7, "Novák")).willReturn(1);
        given(historieService.celkemZaznamu(2026, 7, "Novák")).willReturn(0);

        mockMvc.perform(get("/historie")
                        .param("rok", "2026")
                        .param("mesic", "7")
                        .param("jmeno", "Novák"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rok", 2026))
                .andExpect(model().attribute("mesic", 7))
                .andExpect(model().attribute("jmeno", "Novák"));

        verify(historieService).vyhledej(2026, 7, "Novák", 0);
    }

    // Edge case: stranka daleko za realnym rozsahem vysledku - controller ji jen
    // preda dal, o prazdny vysledek uz se stara HistorieService (viz HistorieServiceTest).
    @Test
    void historieSNadmernouStrankouStalePoslePozadavekDoSluzby() throws Exception {
        given(historieService.vyhledej(null, null, null, 500)).willReturn(List.of());
        given(historieService.celkemStranek(null, null, null)).willReturn(1);
        given(historieService.celkemZaznamu(null, null, null)).willReturn(0);

        mockMvc.perform(get("/historie").param("strana", "500"))
                .andExpect(status().isOk())
                .andExpect(view().name("historie"))
                .andExpect(model().attribute("strana", 500));
    }

    @Test
    void zobrazitPdfZaznamPosleObsahInline() throws Exception {
        given(vygenerovanyDokumentRepository.findById(1L)).willReturn(Optional.of(zaznam(1L, "PDF")));
        given(uloziste.nacti(1L, "PDF")).willReturn("obsah pdf".getBytes());

        mockMvc.perform(get("/historie/1/zobrazit"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("obsah pdf".getBytes()))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")));
    }

    @Test
    void zobrazitWordZaznamOtevreVeVychoziAplikaciAPresmeruje() throws Exception {
        given(vygenerovanyDokumentRepository.findById(2L)).willReturn(Optional.of(zaznam(2L, "WORD")));

        mockMvc.perform(get("/historie/2/zobrazit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/historie"))
                .andExpect(flash().attribute("otevrenyDokument", "Smlouva"));

        verify(uloziste).otevriVeVychoziAplikaci(2L, "WORD");
    }

    @Test
    void zobrazitNeexistujiciZaznamSePresmerujeZpetSHlaskou() throws Exception {
        given(vygenerovanyDokumentRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/historie/999/zobrazit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/historie"))
                .andExpect(flash().attributeExists("chybaHistorie"));
    }

    // Napr. soubor mezitim zmizel z disku, nebo appka bezi bez GUI - uloziste
    // hodi IOException, kterou GlobalExceptionHandler zobrazi na chyba.html.
    @Test
    void zobrazitKdyzOtevreniSelzeVratiChybovouStranku() throws Exception {
        given(vygenerovanyDokumentRepository.findById(2L)).willReturn(Optional.of(zaznam(2L, "WORD")));
        willThrow(new IOException("Vygenerovaný dokument (id 2) na disku neexistuje."))
                .given(uloziste).otevriVeVychoziAplikaci(2L, "WORD");

        mockMvc.perform(get("/historie/2/zobrazit"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("chyba"));
    }
}
