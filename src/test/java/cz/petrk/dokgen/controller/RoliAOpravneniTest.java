package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.config.SecurityConfig;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.RegistraceService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Overuje skutecne vynuceni roli na urovni SecurityConfig (ne jen na urovni
 * jednotlivych controlleru) - ostatni *ControllerTest tridy pouzivaji cisty
 * @WebMvcTest slice bez SecurityConfig, kde @WithMockUser prochazi bez
 * ohledu na roli (jen Boot default "authenticated"). Tady je SecurityConfig
 * explicitne naimportovana, aby se autorizacni pravidla (hasRole("ADMIN")
 * na /sablony a /registrace) opravdu vyhodnotila.
 */
@WebMvcTest(controllers = {SablonaController.class, RegistraceController.class, PrihlaseniController.class})
@Import(SecurityConfig.class)
class RoliAOpravneniTest {

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMaPristupNaSablony() throws Exception {
        given(documentGeneratorService.getDostupneSablony()).willReturn(List.of());

        mockMvc.perform(get("/sablony"))
                .andExpect(status().isOk())
                .andExpect(view().name("sablony"));
    }

    // AccessDeniedHandlerImpl nastavi 403 a forwarduje na /pristup-odepren (viz SecurityConfig) -
    // v @WebMvcTest slice prostredi ale mockovany RequestDispatcher forward doopravdy nededispatchuje
    // (na rozdil od realne bezici appky), takze tu jde overit jen status a cilova URL, ne uz
    // vykresleny obsah stranky /pristup-odepren (ten overuje PrihlaseniControllerTest).
    @Test
    @WithMockUser(roles = "ASISTENTKA")
    void asistentkaNemaPristupNaSablony() throws Exception {
        mockMvc.perform(get("/sablony"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/pristup-odepren"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMaPristupNaRegistraci() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrace"));
    }

    @Test
    @WithMockUser(roles = "ASISTENTKA")
    void asistentkaNemaPristupNaRegistraci() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/pristup-odepren"));
    }
}
