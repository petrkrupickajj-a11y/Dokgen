package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.ResetHeslaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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

@WebMvcTest(ResetHeslaController.class)
@WithMockUser
class ResetHeslaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResetHeslaService resetHeslaService;

    @Test
    void formularZobraziZadostOResetStranku() throws Exception {
        mockMvc.perform(get("/zapomenute-heslo"))
                .andExpect(status().isOk())
                .andExpect(view().name("zapomenute-heslo"));
    }

    @Test
    void odeslatVzdyPresmerujeSPotvrzenimBezOhleduNaExistenciEmailu() throws Exception {
        mockMvc.perform(post("/zapomenute-heslo").with(csrf())
                        .param("email", "kdokoliv@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/zapomenute-heslo"))
                .andExpect(flash().attribute("odeslano", true));

        verify(resetHeslaService).pozadejReset(org.mockito.ArgumentMatchers.eq("kdokoliv@example.com"), anyString());
    }

    @Test
    void formularNovehoHeslaProPlatnyTokenZobraziFormular() throws Exception {
        given(resetHeslaService.jeTokenPlatny("platny-token")).willReturn(true);

        mockMvc.perform(get("/nove-heslo").param("token", "platny-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("nove-heslo"))
                .andExpect(model().attribute("tokenPlatny", true))
                .andExpect(model().attribute("token", "platny-token"));
    }

    @Test
    void formularNovehoHeslaProNeplatnyTokenOznaciHoJakoNeplatny() throws Exception {
        given(resetHeslaService.jeTokenPlatny("spatny-token")).willReturn(false);

        mockMvc.perform(get("/nove-heslo").param("token", "spatny-token"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tokenPlatny", false));
    }

    @Test
    void nastavitNoveHesloPlatneUdajePresmerujeNaLogin() throws Exception {
        mockMvc.perform(post("/nove-heslo").with(csrf())
                        .param("token", "platny-token")
                        .param("noveHeslo", "noveheslo123")
                        .param("noveHesloZnovu", "noveheslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?hesloResetovano"));

        verify(resetHeslaService).nastavNoveHeslo("platny-token", "noveheslo123", "noveheslo123");
    }

    @Test
    void nastavitNoveHesloSNeplatnymTokenemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Odkaz už není platný nebo byl použit."))
                .given(resetHeslaService).nastavNoveHeslo(anyString(), anyString(), anyString());

        mockMvc.perform(post("/nove-heslo").with(csrf())
                        .param("token", "spatny-token")
                        .param("noveHeslo", "noveheslo123")
                        .param("noveHesloZnovu", "noveheslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nove-heslo?token=spatny-token"))
                .andExpect(flash().attribute("chyba", "Odkaz už není platný nebo byl použit."));
    }
}
