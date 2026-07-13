package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.IpOmezovac;
import cz.petrk.dokgen.service.ResetHeslaService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
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

    @MockBean
    private IpOmezovac ipOmezovac;

    @BeforeEach
    void povolIpOmezovacVDefaultu() {
        given(ipOmezovac.povolPozadavek(any())).willReturn(true);
    }

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

    // Zaklad URL odkazu v emailu musi jit z konfigurace, ne z Host hlavicky -
    // ta je plne pod kontrolou odesilatele pozadavku a da se snadno podvrhnout
    // (viz ResetHeslaController).
    @Test
    void odeslatPouzijeNakonfigurovanouUrlIKdyzJePodvrzenaHostHlavicka() throws Exception {
        mockMvc.perform(post("/zapomenute-heslo").with(csrf())
                        .header("Host", "utocnik.example.com")
                        .param("email", "kdokoliv@example.com"))
                .andExpect(status().is3xxRedirection());

        verify(resetHeslaService).pozadejReset("kdokoliv@example.com", "http://localhost:8080");
    }

    @Test
    void odeslatPriPrekroceniLimituIpAdresyVratiChybuANevolaService() throws Exception {
        given(ipOmezovac.povolPozadavek(any())).willReturn(false);

        mockMvc.perform(post("/zapomenute-heslo").with(csrf())
                        .param("email", "kdokoliv@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/zapomenute-heslo"))
                .andExpect(flash().attribute("chyba", "Příliš mnoho požadavků z tvé adresy, zkus to prosím později."));

        verify(resetHeslaService, never()).pozadejReset(any(), any());
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
