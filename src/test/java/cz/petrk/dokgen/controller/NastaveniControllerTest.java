package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@WithMockUser(username = "admin@dokgen.local")
class NastaveniControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MojeEmailService mojeEmailService;

    @Test
    void formularZobraziStrankuNastaveniSAktualnimEmailem() throws Exception {
        mockMvc.perform(get("/nastaveni"))
                .andExpect(status().isOk())
                .andExpect(view().name("nastaveni"))
                .andExpect(model().attribute("aktualniEmail", "admin@dokgen.local"));
    }

    @Test
    void zmenitEmailPlatneUdajePresmerujeNaLoginSPozadavkemZnovuPrihlaseni() throws Exception {
        mockMvc.perform(post("/nastaveni/email").with(csrf())
                        .param("soucasneHeslo", "heslo")
                        .param("novyEmail", "novak@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailZmenen"));

        verify(mojeEmailService).zmenEmail("admin@dokgen.local", "heslo", "novak@example.com");
    }

    @Test
    void zmenitEmailSNespravnymHeslemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Heslo nesouhlasí."))
                .given(mojeEmailService).zmenEmail("admin@dokgen.local", "spatneheslo", "novak@example.com");

        mockMvc.perform(post("/nastaveni/email").with(csrf())
                        .param("soucasneHeslo", "spatneheslo")
                        .param("novyEmail", "novak@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nastaveni"))
                .andExpect(flash().attribute("chyba", "Heslo nesouhlasí."));
    }
}
