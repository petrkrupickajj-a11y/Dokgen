package cz.petrk.dokgen.service;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Napojuje PrihlaseniOmezovac na skutecne prihlasovaci pokusy - Spring
 * Security pri kazdem neuspesnem/uspesnem prihlaseni publikuje udalost,
 * na kterou se tu jen posloucha.
 */
@Component
public class PrihlaseniUdalostiListener {

    private final PrihlaseniOmezovac prihlaseniOmezovac;

    public PrihlaseniUdalostiListener(PrihlaseniOmezovac prihlaseniOmezovac) {
        this.prihlaseniOmezovac = prihlaseniOmezovac;
    }

    @EventListener
    public void naNeuspesnePrihlaseni(AbstractAuthenticationFailureEvent udalost) {
        // Ucet uz je zamceny - LockedException se hazi driv, nez se vubec zkousi
        // heslo, takze tohle by nemel byt skutecny dalsi pokus a nema smysl
        // zamceni prodluzovat.
        if (udalost.getException() instanceof LockedException) {
            return;
        }
        prihlaseniOmezovac.zaznamenejNeuspech(udalost.getAuthentication().getName());
    }

    @EventListener
    public void naUspesnePrihlaseni(AuthenticationSuccessEvent udalost) {
        prihlaseniOmezovac.zaznamenejUspech(udalost.getAuthentication().getName());
    }
}
