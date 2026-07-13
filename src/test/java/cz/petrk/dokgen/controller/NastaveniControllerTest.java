package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeJmenoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(NastaveniController.class)
@WithMockUser(username = "admin")
class NastaveniControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MojeJmenoService mojeJmenoService;

    @Test
    void formularZobraziStrankuNastaveniSAktualnimJmenem() throws Exception {
        mockMvc.perform(get("/nastaveni"))
                .andExpect(status().isOk())
                .andExpect(view().name("nastaveni"))
                .andExpect(model().attribute("aktualniJmeno", "admin"));
    }

    @Test
    void zmenitJmenoPlatneUdajePresmerujeNaLoginSPozadavkemZnovuPrihlaseni() throws Exception {
        mockMvc.perform(post("/nastaveni/jmeno").with(csrf())
                        .param("soucasneHeslo", "heslo")
                        .param("noveJmeno", "novak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?jmenoZmeneno"));

        verify(mojeJmenoService).zmenJmeno("admin", "heslo", "novak");
    }

    @Test
    void zmenitJmenoSNespravnymHeslemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Heslo nesouhlasí."))
                .given(mojeJmenoService).zmenJmeno("admin", "spatneheslo", "novak");

        mockMvc.perform(post("/nastaveni/jmeno").with(csrf())
                        .param("soucasneHeslo", "spatneheslo")
                        .param("noveJmeno", "novak"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nastaveni"))
                .andExpect(flash().attribute("chyba", "Heslo nesouhlasí."));
    }
}
