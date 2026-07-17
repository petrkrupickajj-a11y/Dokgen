package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.service.SpravaUctuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

@WebMvcTest(UzivateleController.class)
@WithMockUser(username = "admin@dokgen.local")
class UzivateleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpravaUctuService spravaUctuService;

    @Test
    void seznamZobraziCekajiciIAktivniUcty() throws Exception {
        List<Uzivatel> cekajici = List.of(new Uzivatel("novak@example.com", "hash", false));
        List<Uzivatel> aktivni = List.of(new Uzivatel("admin@dokgen.local", "hash"));
        given(spravaUctuService.getCekajiciUcty()).willReturn(cekajici);
        given(spravaUctuService.getAktivniUcty()).willReturn(aktivni);

        mockMvc.perform(get("/uzivatele"))
                .andExpect(status().isOk())
                .andExpect(view().name("uzivatele"))
                .andExpect(model().attribute("cekajici", cekajici))
                .andExpect(model().attribute("aktivni", aktivni))
                .andExpect(model().attribute("aktualniEmail", "admin@dokgen.local"));
    }

    @Test
    void schvalitPresmerujeZpetNaSeznam() throws Exception {
        mockMvc.perform(post("/uzivatele/{id}/schvalit", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/uzivatele"));

        verify(spravaUctuService).schval(1L);
    }

    @Test
    void schvalitSChybouJiVratiVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Uživatel neexistuje.")).given(spravaUctuService).schval(1L);

        mockMvc.perform(post("/uzivatele/{id}/schvalit", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/uzivatele"))
                .andExpect(flash().attribute("chyba", "Uživatel neexistuje."));
    }

    @Test
    void zamitnoutPresmerujeZpetNaSeznam() throws Exception {
        mockMvc.perform(post("/uzivatele/{id}/zamitnout", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/uzivatele"));

        verify(spravaUctuService).zamitni(1L);
    }

    @Test
    void smazatPresmerujeZpetNaSeznam() throws Exception {
        mockMvc.perform(post("/uzivatele/{id}/smazat", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/uzivatele"));

        verify(spravaUctuService).smaz(1L, "admin@dokgen.local");
    }

    @Test
    void smazatSChybouJiVratiVeFlashAtributu() throws Exception {
        willThrow(new IllegalArgumentException("Nemůžeš smazat účet, pod kterým jsi právě přihlášený."))
                .given(spravaUctuService).smaz(1L, "admin@dokgen.local");

        mockMvc.perform(post("/uzivatele/{id}/smazat", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/uzivatele"))
                .andExpect(flash().attribute("chyba", "Nemůžeš smazat účet, pod kterým jsi právě přihlášený."));
    }
}
