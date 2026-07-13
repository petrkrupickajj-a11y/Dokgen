package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MojeJmenoService {

    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource zpravy;

    public MojeJmenoService(UzivatelRepository uzivatelRepository, PasswordEncoder passwordEncoder,
                             MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    /**
     * Zmeni uzivatelske jmeno prihlaseneho uctu - vyzaduje spravne zadane
     * soucasne heslo. Vraci ulozene (orizle) nove jmeno, aby ho controller
     * mohl poslat do "prihlas se znovu" hlasky.
     */
    public String zmenJmeno(String aktualniJmeno, String soucasneHeslo, String noveJmeno) {
        Uzivatel uzivatel = uzivatelRepository.findByJmeno(aktualniJmeno)
                .orElseThrow(() -> new IllegalStateException("Přihlášený uživatel \"" + aktualniJmeno + "\" nebyl nalezen."));

        if (!passwordEncoder.matches(soucasneHeslo == null ? "" : soucasneHeslo, uzivatel.getHeslo())) {
            throw new IllegalArgumentException(zprava("chyba.moje_jmeno.heslo_nesouhlasi"));
        }
        if (noveJmeno == null || noveJmeno.isBlank()) {
            throw new IllegalArgumentException(zprava("chyba.moje_jmeno.jmeno_povinne"));
        }

        String ocisteneJmeno = noveJmeno.trim();
        if (!ocisteneJmeno.equals(aktualniJmeno) && uzivatelRepository.existsByJmeno(ocisteneJmeno)) {
            throw new IllegalArgumentException(zprava("chyba.moje_jmeno.jmeno_obsazene", ocisteneJmeno));
        }

        uzivatel.setJmeno(ocisteneJmeno);
        uzivatelRepository.save(uzivatel);
        return ocisteneJmeno;
    }
}
