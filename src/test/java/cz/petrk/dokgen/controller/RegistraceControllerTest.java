package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.RegistraceService;
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

    @Test
    void formularZobraziRegistracniStranku() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrace"));
    }

    @Test
    void zaregistrovatPlatneUdajePresmerujeSUspechem() throws Exception {
        mockMvc.perform(post("/registrace").with(csrf())
                        .param("jmeno", "novak")
                        .param("heslo", "tajneheslo")
                        .param("hesloZnovu", "tajneheslo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("uspech", "novak"));

        verify(registraceService).zaregistruj("novak", "tajneheslo", "tajneheslo", "ASISTENTKA");
    }

    @Test
    void zaregistrovatSVybranouRoliJiPredaSluzbe() throws Exception {
        mockMvc.perform(post("/registrace").with(csrf())
                        .param("jmeno", "novak")
                        .param("heslo", "tajneheslo")
                        .param("hesloZnovu", "tajneheslo")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"));

        verify(registraceService).zaregistruj("novak", "tajneheslo", "tajneheslo", "ADMIN");
    }

    @Test
    void zaregistrovatSNeshodujicimiSeHesyVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Hesla se neshodují."))
                .given(registraceService).zaregistruj("novak", "heslo1", "heslo2", "ASISTENTKA");

        mockMvc.perform(post("/registrace").with(csrf())
                        .param("jmeno", "novak")
                        .param("heslo", "heslo1")
                        .param("hesloZnovu", "heslo2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("chyba", "Hesla se neshodují."))
                .andExpect(flash().attribute("zadaneJmeno", "novak"));
    }

    @Test
    void zaregistrovatSObsazenymJmenemVratiChybuVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Uživatelské jméno \"admin\" už je obsazené."))
                .given(registraceService).zaregistruj("admin", "tajneheslo", "tajneheslo", "ASISTENTKA");

        mockMvc.perform(post("/registrace").with(csrf())
                        .param("jmeno", "admin")
                        .param("heslo", "tajneheslo")
                        .param("hesloZnovu", "tajneheslo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attribute("chyba", "Uživatelské jméno \"admin\" už je obsazené."));
    }
}
