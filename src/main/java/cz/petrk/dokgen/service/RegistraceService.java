package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Role;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RegistraceService {

    private static final int MIN_DELKA_HESLA = 6;

    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource zpravy;

    public RegistraceService(UzivatelRepository uzivatelRepository, PasswordEncoder passwordEncoder,
                              MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    public void zaregistruj(String jmeno, String heslo, String hesloZnovu, String role) {
        if (jmeno == null || jmeno.isBlank()) {
            throw new IllegalArgumentException(zprava("chyba.registrace.jmeno_povinne"));
        }
        if (heslo == null || heslo.length() < MIN_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.registrace.heslo_kratke", MIN_DELKA_HESLA));
        }
        if (!heslo.equals(hesloZnovu)) {
            throw new IllegalArgumentException(zprava("chyba.registrace.hesla_neshoda"));
        }

        String ocisteneJmeno = jmeno.trim();
        if (uzivatelRepository.existsByJmeno(ocisteneJmeno)) {
            throw new IllegalArgumentException(zprava("chyba.registrace.jmeno_obsazene", ocisteneJmeno));
        }

        uzivatelRepository.save(new Uzivatel(ocisteneJmeno, passwordEncoder.encode(heslo), zpracujRoli(role)));
    }

    private Role zpracujRoli(String role) {
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(zprava("chyba.registrace.role_neplatna"));
        }
    }
}
