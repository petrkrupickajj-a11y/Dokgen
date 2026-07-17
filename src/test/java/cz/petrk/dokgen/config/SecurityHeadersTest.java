package cz.petrk.dokgen.config;

import cz.petrk.dokgen.controller.ZdraviController;
import cz.petrk.dokgen.service.IpOmezovac;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Overuje, ze SecurityConfig opravdu posila Content-Security-Policy
 * hlavicku na kazdou odpoved - i tu neprihlasenou (/zdravi je permitAll,
 * viz VerejnyPristupTest).
 */
@WebMvcTest(controllers = ZdraviController.class)
@Import(SecurityConfig.class)
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IpOmezovac ipOmezovac;

    @Test
    void odpovedObsahujeContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/zdravi"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
    }
}
