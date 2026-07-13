package cz.petrk.dokgen.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * jeAdmin() rozhoduje, jestli se v nav zobrazi odkaz "Sablony" (viz
 * seznam.html, sablony.html, historie.html, generovat.html, verze.html) -
 * ASISTENTKA na ne stejne nema pristup (SecurityConfig), takze by tam
 * jinak videla jen odkaz vedouci na "Pristup odepren".
 */
class NavigaceModelAdviceTest {

    private final NavigaceModelAdvice advice = new NavigaceModelAdvice();

    @Test
    void adminMaJeAdminTrue() {
        var autentizace = new UsernamePasswordAuthenticationToken("admin", "heslo",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(advice.jeAdmin(autentizace)).isTrue();
    }

    @Test
    void asistentkaMaJeAdminFalse() {
        var autentizace = new UsernamePasswordAuthenticationToken("asistentka", "heslo",
                List.of(new SimpleGrantedAuthority("ROLE_ASISTENTKA")));

        assertThat(advice.jeAdmin(autentizace)).isFalse();
    }

    @Test
    void neprihlasenyUzivatelMaJeAdminFalse() {
        assertThat(advice.jeAdmin(null)).isFalse();
    }

    @Test
    void vicerolovyUcetSAdminMeziAutoritamiMaJeAdminTrue() {
        var autentizace = new UsernamePasswordAuthenticationToken("kombinovany", "heslo",
                List.of(new SimpleGrantedAuthority("ROLE_ASISTENTKA"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(advice.jeAdmin(autentizace)).isTrue();
    }
}
