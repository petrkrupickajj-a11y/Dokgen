package cz.petrk.dokgen.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PrihlaseniUdalostiListener napojuje PrihlaseniOmezovac na skutecne
 * prihlasovaci udalosti Spring Security. Klicova je vetev pro LockedException
 * (viz naNeuspesnePrihlaseni) - kdyz je ucet uz zamceny, Spring Security
 * odmitne prihlaseni jeste pred kontrolou hesla, takze by to nemel byt
 * pocitan jako dalsi neuspesny pokus (jinak by se zamceni umyslne
 * prodluzovalo pri kazdem pokusu behem 15minutove blokace).
 */
class PrihlaseniUdalostiListenerTest {

    private final PrihlaseniOmezovac prihlaseniOmezovac = Mockito.mock(PrihlaseniOmezovac.class);
    private final PrihlaseniUdalostiListener listener = new PrihlaseniUdalostiListener(prihlaseniOmezovac);

    @Test
    void neuspesnePrihlaseniZaznamenaNeuspech() {
        var autentizace = new UsernamePasswordAuthenticationToken("novak", "spatneheslo");
        var udalost = new AuthenticationFailureBadCredentialsEvent(autentizace, new BadCredentialsException("Špatné heslo"));

        listener.naNeuspesnePrihlaseni(udalost);

        verify(prihlaseniOmezovac).zaznamenejNeuspech("novak");
    }

    @Test
    void neuspechNaJizZamcenemUctuSeNezaznamenaAbySeZamceniNeprodluzovalo() {
        var autentizace = new UsernamePasswordAuthenticationToken("novak", "cokoliv");
        var udalost = new AuthenticationFailureLockedEvent(autentizace, new LockedException("Účet je zamčený"));

        listener.naNeuspesnePrihlaseni(udalost);

        verify(prihlaseniOmezovac, never()).zaznamenejNeuspech(any());
    }

    @Test
    void uspesnePrihlaseniZaznamenaUspech() {
        var autentizace = new UsernamePasswordAuthenticationToken("novak", "spravneheslo");
        var udalost = new AuthenticationSuccessEvent(autentizace);

        listener.naUspesnePrihlaseni(udalost);

        verify(prihlaseniOmezovac).zaznamenejUspech("novak");
    }
}
