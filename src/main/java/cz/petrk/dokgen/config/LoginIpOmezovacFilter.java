package cz.petrk.dokgen.config;

import cz.petrk.dokgen.service.IpOmezovac;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * PrihlaseniOmezovac limituje pokusy o prihlaseni jen podle emailu - jedna IP
 * adresa tak muze bez omezeni zkouset hesla napric mnoha ruznymi emaily
 * najednou. Tenhle filtr to doplnuje o limit na POST /login podle IP adresy
 * (stejny IpOmezovac jako /registrace a /zapomenute-heslo), jeste pred tim,
 * nez se pozadavek vubec dostane k Spring Security autentizaci.
 */
public class LoginIpOmezovacFilter extends OncePerRequestFilter {

    private final IpOmezovac ipOmezovac;

    public LoginIpOmezovacFilter(IpOmezovac ipOmezovac) {
        this.ipOmezovac = ipOmezovac;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean jePrihlaseni = "POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath());
        if (jePrihlaseni && !ipOmezovac.povolPozadavek(request.getRemoteAddr())) {
            response.sendRedirect(request.getContextPath() + "/login?prilisMnohoPozadavku");
            return;
        }
        chain.doFilter(request, response);
    }
}
