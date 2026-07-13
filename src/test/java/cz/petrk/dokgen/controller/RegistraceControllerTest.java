package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.IpOmezovac;
import cz.petrk.dokgen.service.RegistraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RegistraceController.class)
@WithMockUser
class RegistraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistraceService registraceService;

    @MockBean
    private IpOmezovac ipOmezovac;

    @BeforeEach
    void povolIpOmezovacVDefaultu() {
        given(ipOmezovac.povolPozadavek(any())).willReturn(true);
    }

    @Test
    void formularZobraziRegistracniStranku() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrace"));
    }

    @Test
    void zaregistrovatPlatneUdajePresmerujeNaLoginSPotvrzenim() throws Exception {
        mockMvc.perform(post("/registrace").with(csrf())
                        .param("email", "novak@example.com")
                        .param("heslo", "tajneheslo123")
                        .param("hesloZnovu", "tajneheslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrovano"));

        verify(registraceService).zaregistruj("novak@example.com", "tajneheslo123", "tajneheslo123");
    }

    @Test
    void zaregistrovatSNeshodujicimiSeHesyVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Hesla se neshodují."))
                .given(registraceService).zaregistruj("novak@example.com", "heslo1", "heslo2");

        mockMvc.perform(post("/registrace").with(csrf())
                        .param("email", "novak@example.com")
                        .param("heslo", "heslo1")
                        .param("hesloZnovu", "heslo2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("chyba", "Hesla se neshodují."))
                .andExpect(flash().attribute("zadanyEmail", "novak@example.com"));
    }

    @Test
    void zaregistrovatSObsazenymEmailemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Účet s emailem \"admin@dokgen.local\" už existuje."))
                .given(registraceService).zaregistruj("admin@dokgen.local", "tajneheslo123", "tajneheslo123");

        mockMvc.perform(post("/registrace").with(csrf())
                        .param("email", "admin@dokgen.local")
                        .param("heslo", "tajneheslo123")
                        .param("hesloZnovu", "tajneheslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("chyba", "Účet s emailem \"admin@dokgen.local\" už existuje."));
    }

    @Test
    void zaregistrovatPriPrekroceniLimituIpAdresyVratiChybuANevolaService() throws Exception {
        given(ipOmezovac.povolPozadavek(any())).willReturn(false);

        mockMvc.perform(post("/registrace").with(csrf())
                        .param("email", "novak@example.com")
                        .param("heslo", "tajneheslo123")
                        .param("hesloZnovu", "tajneheslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("chyba", "Příliš mnoho požadavků z tvé adresy, zkus to prosím později."));

        verify(registraceService, never()).zaregistruj(any(), any(), any());
    }
}
