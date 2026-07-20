package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistraceService {

    private static final int MIN_DELKA_HESLA = 8;
    // BCrypt (viz PasswordEncoder) pracuje jen s prvnimi 72 bajty hesla a novejsi
    // Spring Security u delsich hesel rovnou vyhodi vyjimku - delsi heslo proto
    // odmitneme uz tady se srozumitelnou hlaskou.
    private static final int MAX_DELKA_HESLA = 72;

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

    /**
     * Verejna registrace noveho uctu - ucet je hned po zalozeni pouzitelny
     * a ma stejna opravneni jako kterykoliv jiny (viz DokgenUserDetailsService).
     */
    public void zaregistruj(String email, String heslo, String hesloZnovu) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(zprava("chyba.registrace.email_povinny"));
        }
        String ocistenyEmail = EmailValidace.normalizuj(email);
        if (!EmailValidace.jePlatny(ocistenyEmail)) {
            throw new IllegalArgumentException(zprava("chyba.registrace.email_format"));
        }
        if (heslo == null || heslo.length() < MIN_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.registrace.heslo_kratke", MIN_DELKA_HESLA));
        }
        if (heslo.length() > MAX_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.registrace.heslo_dlouhe", MAX_DELKA_HESLA));
        }
        if (!heslo.equals(hesloZnovu)) {
            throw new IllegalArgumentException(zprava("chyba.registrace.hesla_neshoda"));
        }
        if (uzivatelRepository.existsByEmail(ocistenyEmail)) {
            throw new IllegalArgumentException(zprava("chyba.registrace.email_obsazeny", ocistenyEmail));
        }

        uzivatelRepository.save(new Uzivatel(ocistenyEmail, passwordEncoder.encode(heslo)));
    }
}
