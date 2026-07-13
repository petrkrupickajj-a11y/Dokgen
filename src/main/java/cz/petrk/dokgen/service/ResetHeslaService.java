package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.ResetHesla;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.ResetHeslaRepository;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Reset zapomenuteho hesla pres jednorazovy token zaslany emailem (viz
 * ResetHesla, EmailOdesilatel). Appka se navenek chova stejne bez ohledu na
 * to, jestli zadany email v databazi existuje - nesmi prozradit, ktere
 * emaily jsou registrovane (viz pozadejReset).
 */
@Service
public class ResetHeslaService {

    private static final Logger LOG = LoggerFactory.getLogger(ResetHeslaService.class);
    private static final int MIN_DELKA_HESLA = 8;
    private static final Duration PLATNOST = Duration.ofMinutes(45);

    private final UzivatelRepository uzivatelRepository;
    private final ResetHeslaRepository resetHeslaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOdesilatel emailOdesilatel;
    private final MessageSource zpravy;
    private final Clock hodiny;
    private final SecureRandom nahoda = new SecureRandom();

    public ResetHeslaService(UzivatelRepository uzivatelRepository,
                              ResetHeslaRepository resetHeslaRepository,
                              PasswordEncoder passwordEncoder,
                              EmailOdesilatel emailOdesilatel,
                              MessageSource zpravy,
                              Clock hodiny) {
        this.uzivatelRepository = uzivatelRepository;
        this.resetHeslaRepository = resetHeslaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailOdesilatel = emailOdesilatel;
        this.zpravy = zpravy;
        this.hodiny = hodiny;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    /**
     * Pozada o reset hesla - pokud ucet s timhle emailem existuje, posle mu
     * na email odkaz s tokenem (platnym 45 minut). Nezalezi na vysledku
     * vyhledani - appka se navenek chova uplne stejne v obou pripadech,
     * volajici (ResetHeslaController) tak muze uzivateli vzdy ukazat
     * stejnou zpravu.
     */
    public void pozadejReset(String email, String zakladUrl) {
        if (email == null || email.isBlank()) {
            return;
        }
        uzivatelRepository.findByEmail(EmailValidace.normalizuj(email)).ifPresent(uzivatel -> {
            String token = vygenerujToken();
            resetHeslaRepository.save(new ResetHesla(uzivatel, hash(token), LocalDateTime.now(hodiny).plus(PLATNOST)));

            String odkaz = zakladUrl + "/nove-heslo?token=" + token;
            boolean odeslano = emailOdesilatel.odesli(uzivatel.getEmail(),
                    zprava("email.reset.predmet"),
                    zprava("email.reset.telo", odkaz));

            if (!odeslano) {
                LOG.warn("""


                        Email s odkazem na reset hesla se nepodařilo odeslat (SMTP není nastavené
                        nebo selhalo) - uživatel "{}" si může heslo přesto změnit přes tenhle odkaz:

                            {}

                        Platnost odkazu je 45 minut od vyžádání.
                        """, uzivatel.getEmail(), odkaz);
            }
        });
    }

    /** Vraci true, pokud je token platny (existuje, nevyprsel, nebyl uz pouzity). */
    public boolean jeTokenPlatny(String token) {
        return najdiPlatnyToken(token).isPresent();
    }

    /** Nastavi nove heslo pro ucet patrici k platnemu tokenu a token spotrebuje. */
    public void nastavNoveHeslo(String token, String noveHeslo, String noveHesloZnovu) {
        ResetHesla resetHesla = najdiPlatnyToken(token)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.reset_hesla.token_neplatny")));

        if (noveHeslo == null || noveHeslo.length() < MIN_DELKA_HESLA) {
            throw new IllegalArgumentException(zprava("chyba.reset_hesla.heslo_kratke", MIN_DELKA_HESLA));
        }
        if (!noveHeslo.equals(noveHesloZnovu)) {
            throw new IllegalArgumentException(zprava("chyba.reset_hesla.hesla_neshoda"));
        }

        Uzivatel uzivatel = resetHesla.getUzivatel();
        uzivatel.setHeslo(passwordEncoder.encode(noveHeslo));
        uzivatelRepository.save(uzivatel);

        resetHesla.setPouzit(true);
        resetHeslaRepository.save(resetHesla);
    }

    private Optional<ResetHesla> najdiPlatnyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return resetHeslaRepository.findByTokenHash(hash(token))
                .filter(reset -> !reset.isPouzit())
                .filter(reset -> reset.getVyprsiDne().isAfter(LocalDateTime.now(hodiny)));
    }

    private String vygenerujToken() {
        byte[] bajty = new byte[32];
        nahoda.nextBytes(bajty);
        return HexFormat.of().formatHex(bajty);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 není k dispozici", e);
        }
    }
}
