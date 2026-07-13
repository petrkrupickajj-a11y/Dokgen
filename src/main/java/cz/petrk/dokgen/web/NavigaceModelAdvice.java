package cz.petrk.dokgen.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Prida do modelu kazde vykreslene stranky atribut "jeAdmin", aby si
 * navigacni sablony (nav v seznam.html, sablony.html...) mohly schovat
 * odkaz na "Šablony" pred uzivatelem s roli ASISTENTKA - ta na /sablony
 * stejne nema pristup (viz SecurityConfig), takze by tam jinak videla
 * odkaz vedouci jen na stranku "Přístup odepřen".
 */
@ControllerAdvice
public class NavigaceModelAdvice {

    @ModelAttribute("jeAdmin")
    public boolean jeAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(autorita -> autorita.getAuthority().equals("ROLE_ADMIN"));
    }
}
