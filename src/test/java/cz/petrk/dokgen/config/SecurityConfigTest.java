package cz.petrk.dokgen.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Overuje, ze neschvaleny ucet (DisabledException, viz DokgenUserDetailsService)
 * skonci na jine strance nez obycejne spatne heslo - login.html podle toho
 * ukaze jinou hlasku.
 */
class SecurityConfigTest {

    private final AuthenticationFailureHandler handler = new SecurityConfig().authenticationFailureHandler();

    @Test
    void neschvalenyUcetPresmerujeNaHlaskuCekaNaSchvaleni() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        given(request.getContextPath()).willReturn("");

        handler.onAuthenticationFailure(request, response, new DisabledException("neaktivni"));

        verify(response).sendRedirect("/login?cekaNaSchvaleni");
    }

    @Test
    void spatneHesloPresmerujeNaObecnouChybu() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        given(request.getContextPath()).willReturn("");

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("spatne heslo"));

        verify(response).sendRedirect("/login?error");
    }
}
