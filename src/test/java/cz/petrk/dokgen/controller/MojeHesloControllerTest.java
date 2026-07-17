package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeHesloService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MojeHesloController.class)
@WithMockUser(username = "admin")
class MojeHesloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MojeHesloService mojeHesloService;

    @Test
    void formularZobraziStrankuZmenyHesla() throws Exception {
        mockMvc.perform(get("/moje-heslo"))
                .andExpect(status().isOk())
                .andExpect(view().name("moje-heslo"));
    }

    @Test
    void zmenitPlatneUdajePresmerujeSUspechem() throws Exception {
        mockMvc.perform(post("/moje-heslo").with(csrf())
                        .param("soucasneHeslo", "stareheslo")
                        .param("noveHeslo", "noveheslo")
                        .param("noveHesloZnovu", "noveheslo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moje-heslo"))
                .andExpect(flash().attribute("uspech", true));

        verify(mojeHesloService).zmenHeslo("admin", "stareheslo", "noveheslo", "noveheslo");
    }

    @Test
    void zmenitSNespravnymSoucasnymHeslemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Současné heslo nesouhlasí."))
                .given(mojeHesloService).zmenHeslo("admin", "spatneheslo", "noveheslo", "noveheslo");

        mockMvc.perform(post("/moje-heslo").with(csrf())
                        .param("soucasneHeslo", "spatneheslo")
                        .param("noveHeslo", "noveheslo")
                        .param("noveHesloZnovu", "noveheslo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moje-heslo"))
                .andExpect(flash().attribute("chyba", "Současné heslo nesouhlasí."));
    }
}
