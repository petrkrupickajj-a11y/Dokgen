package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MojeHesloService {

    private static final int MIN_DELKA_HESLA = 8;
    // BCrypt (viz PasswordEncoder) pracuje jen s prvnimi 72 bajty hesla a novejsi
    // Spring Security u delsich hesel rovnou vyhodi vyjimku - delsi heslo proto
    // odmitneme uz tady se srozumitelnou hlaskou.
    private static final int MAX_DELKA_HESLA = 72;

    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource zpravy;

    public MojeHesloService(UzivatelRepository uzivatelRepository, PasswordEncoder passwordEncoder,
                             MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    /** Zmeni heslo prihlaseneho uzivatele - vyzaduje spravne zadane soucasne heslo. */
    public void zmenHeslo(String email, String soucasneHeslo, String noveHeslo, String noveHesloZnovu) {
        Uzivatel uzivatel = uzivatelRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Přihlášený uživatel \"" + email + "\" nebyl nalezen."));

        if (!passwordEncoder.matches(soucasneHeslo == null ? "" : soucasneHeslo, uzivatel.getHeslo())) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.soucasne_nesouhlasi"));
        }
        if (noveHeslo == null || noveHeslo.length() < MIN_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.heslo_kratke", MIN_DELKA_HESLA));
        }
        if (noveHeslo.length() > MAX_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.heslo_dlouhe", MAX_DELKA_HESLA));
        }
        if (!noveHeslo.equals(noveHesloZnovu)) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.hesla_neshoda"));
        }

        uzivatel.setHeslo(passwordEncoder.encode(noveHeslo));
        uzivatelRepository.save(uzivatel);
    }
}
