package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.config.SecurityConfig;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.IpOmezovac;
import cz.petrk.dokgen.service.MojeEmailService;
import cz.petrk.dokgen.service.MojeHesloService;
import cz.petrk.dokgen.service.RegistraceService;
import cz.petrk.dokgen.service.ResetHeslaService;
import cz.petrk.dokgen.service.SablonaUlozisteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Overuje skutecne vynuceni pristupovych pravidel na urovni SecurityConfig
 * (ne jen na urovni jednotlivych controlleru) - ostatni *ControllerTest
 * tridy pouzivaji cisty @WebMvcTest slice bez SecurityConfig, kde
 * @WithMockUser prochazi bez ohledu na to, jestli je stranka ve skutecnosti
 * permitAll. Tady je SecurityConfig explicitne naimportovana, aby se
 * permitAll pravidla opravdu vyhodnotila.
 */
@WebMvcTest(controllers = {SablonaController.class, RegistraceController.class, PrihlaseniController.class, MojeHesloController.class, NastaveniController.class, ResetHeslaController.class, ZdraviController.class})
@Import(SecurityConfig.class)
class VerejnyPristupTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentGeneratorService documentGeneratorService;

    @MockBean
    private SablonaRepository sablonaRepository;

    @MockBean
    private SablonaUlozisteService uloziste;

    @MockBean
    private RegistraceService registraceService;

    @MockBean
    private MojeHesloService mojeHesloService;

    @MockBean
    private MojeEmailService mojeEmailService;

    @MockBean
    private ResetHeslaService resetHeslaService;

    @MockBean
    private IpOmezovac ipOmezovac;

    // Kazdy prihlaseny ucet ma stejna opravneni - /sablony vyzaduje jen prihlaseni, ne konkretni roli.
    @Test
    @WithMockUser
    void prihlasenyUzivatelMaPristupNaSablony() throws Exception {
        given(documentGeneratorService.getDostupneSablony()).willReturn(List.of());

        mockMvc.perform(get("/sablony"))
                .andExpect(status().isOk())
                .andExpect(view().name("sablony"));
    }

    // Registrace je verejna - i uplne neprihlaseny navstevnik se musi dostat
    // na formular (viz SecurityConfig .permitAll na /registrace).
    @Test
    void registraceJePristupnaINeprihlasenemu() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrace"));
    }

    @Test
    @WithMockUser
    void prihlasenyUzivatelMaPristupNaZmenuVlastnihoHesla() throws Exception {
        mockMvc.perform(get("/moje-heslo"))
                .andExpect(status().isOk())
                .andExpect(view().name("moje-heslo"));
    }

    @Test
    @WithMockUser
    void prihlasenyUzivatelMaPristupNaNastaveni() throws Exception {
        mockMvc.perform(get("/nastaveni"))
                .andExpect(status().isOk())
                .andExpect(view().name("nastaveni"));
    }

    // Zapomenute heslo musi jit pouzit i bez prihlaseni - k tomu to cele je.
    @Test
    void zapomenuteHesloJePristupneINeprihlasenemu() throws Exception {
        mockMvc.perform(get("/zapomenute-heslo"))
                .andExpect(status().isOk())
                .andExpect(view().name("zapomenute-heslo"));
    }

    @Test
    void noveHesloJePristupneINeprihlasenemu() throws Exception {
        mockMvc.perform(get("/nove-heslo").param("token", "cokoliv"))
                .andExpect(status().isOk())
                .andExpect(view().name("nove-heslo"));
    }

    // /zdravi musi jit zavolat bez prihlaseni - hodi se ho anonymne, jeste
    // pred spustenim appky (viz DokgenApplication.main()).
    @Test
    void zdraviJePristupneINeprihlasenemu() throws Exception {
        mockMvc.perform(get("/zdravi"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
