package cz.petrk.dokgen.config;

import cz.petrk.dokgen.service.IpOmezovac;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Overuje, ze filtr blokuje POST /login po prekroceni IpOmezovac limitu a
 * jinak (GET /login, jine cesty, nebo dokud limit nepresahne) pozadavek
 * beze zmeny propusti dal - stejny princip jako u /registrace.
 */
class LoginIpOmezovacFilterTest {

    private final IpOmezovac ipOmezovac = Mockito.mock(IpOmezovac.class);
    private final LoginIpOmezovacFilter filter = new LoginIpOmezovacFilter(ipOmezovac);

    @Test
    void povoleneIpProjdeDal() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        given(request.getMethod()).willReturn("POST");
        given(request.getServletPath()).willReturn("/login");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(ipOmezovac.povolPozadavek("1.2.3.4")).willReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(Mockito.anyString());
    }

    @Test
    void prekroceniLimituPresmerujeAZastaviRetezec() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        given(request.getMethod()).willReturn("POST");
        given(request.getServletPath()).willReturn("/login");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(request.getContextPath()).willReturn("");
        given(ipOmezovac.povolPozadavek("1.2.3.4")).willReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/login?prilisMnohoPozadavku");
        verify(chain, never()).doFilter(Mockito.any(), Mockito.any());
    }

    @Test
    void getNaLoginSeNekontroluje() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        given(request.getMethod()).willReturn("GET");
        given(request.getServletPath()).willReturn("/login");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(ipOmezovac, never()).povolPozadavek(Mockito.anyString());
    }

    @Test
    void jinaCestaSeNekontroluje() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        given(request.getMethod()).willReturn("POST");
        given(request.getServletPath()).willReturn("/registrace");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(ipOmezovac, never()).povolPozadavek(Mockito.anyString());
    }
}
