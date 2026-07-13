package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MojeHesloService {

    private static final int MIN_DELKA_HESLA = 6;

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
    public void zmenHeslo(String jmeno, String soucasneHeslo, String noveHeslo, String noveHesloZnovu) {
        Uzivatel uzivatel = uzivatelRepository.findByJmeno(jmeno)
                .orElseThrow(() -> new IllegalStateException("Přihlášený uživatel \"" + jmeno + "\" nebyl nalezen."));

        if (!passwordEncoder.matches(soucasneHeslo == null ? "" : soucasneHeslo, uzivatel.getHeslo())) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.soucasne_nesouhlasi"));
        }
        if (noveHeslo == null || noveHeslo.length() < MIN_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.heslo_kratke", MIN_DELKA_HESLA));
        }
        if (!noveHeslo.equals(noveHesloZnovu)) {
            throw new IllegalArgumentException(zprava("chyba.moje_heslo.hesla_neshoda"));
        }

        uzivatel.setHeslo(passwordEncoder.encode(noveHeslo));
        uzivatelRepository.save(uzivatel);
    }
}
