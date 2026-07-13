package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MojeEmailService {

    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource zpravy;

    public MojeEmailService(UzivatelRepository uzivatelRepository, PasswordEncoder passwordEncoder,
                             MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    /**
     * Zmeni prihlasovaci email prihlaseneho uctu - vyzaduje spravne zadane
     * soucasne heslo. Vraci ulozeny (orizly) novy email, aby ho controller
     * mohl poslat do "prihlas se znovu" hlasky.
     */
    public String zmenEmail(String aktualniEmail, String soucasneHeslo, String novyEmail) {
        String normalizovanyAktualniEmail = EmailValidace.normalizuj(aktualniEmail);
        Uzivatel uzivatel = uzivatelRepository.findByEmail(normalizovanyAktualniEmail)
                .orElseThrow(() -> new IllegalStateException("Přihlášený uživatel \"" + aktualniEmail + "\" nebyl nalezen."));

        if (!passwordEncoder.matches(soucasneHeslo == null ? "" : soucasneHeslo, uzivatel.getHeslo())) {
            throw new IllegalArgumentException(zprava("chyba.moje_email.heslo_nesouhlasi"));
        }
        if (novyEmail == null || novyEmail.isBlank()) {
            throw new IllegalArgumentException(zprava("chyba.moje_email.email_povinny"));
        }

        String ocistenyEmail = EmailValidace.normalizuj(novyEmail);
        if (!EmailValidace.jePlatny(ocistenyEmail)) {
            throw new IllegalArgumentException(zprava("chyba.moje_email.email_format"));
        }
        if (!ocistenyEmail.equals(normalizovanyAktualniEmail) && uzivatelRepository.existsByEmail(ocistenyEmail)) {
            throw new IllegalArgumentException(zprava("chyba.moje_email.email_obsazeny", ocistenyEmail));
        }

        uzivatel.setEmail(ocistenyEmail);
        uzivatelRepository.save(uzivatel);
        return ocistenyEmail;
    }
}
