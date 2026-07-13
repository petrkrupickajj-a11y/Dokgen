package cz.petrk.dokgen.service;

import cz.petrk.dokgen.util.EmailValidace;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jednoducha ochrana proti automatizovanemu zkouseni hesel na /login - drzi
 * si v pameti pocet neuspesnych pokusu na kazdy email (i neexistujici -
 * DaoAuthenticationProvider hazi stejnou udalost pro spatne heslo i pro
 * neznamy ucet, takze nejde poznat rozdil a appka tak nikomu nepradzi,
 * ktere emaily v ni existuji). Po prekroceni limitu je email docasne
 * "zamceny" - DokgenUserDetailsService pro nej vrati ucet s priznakem
 * accountLocked, takze Spring Security samo odmitne prihlaseni jeste pred
 * kontrolou hesla (LockedException).
 *
 * Zaznamy zijou jen v pameti procesu - restart appky je vynuluje. Pro appku
 * pouzivanou jednotkami lidi v ramci jedne firmy je to dostatecna ochrana
 * bez potreby externiho uloziste (Redis apod.).
 *
 * Mapa by jinak rostla neomezene - kazdy pokus o zkoumani nahodnych/neexistujicich
 * emailu by v ni navzdy zustal zaznam. zaznamenejNeuspech proto pri kazdem zapisu
 * promaze zaznamy, u kterych od posledni aktivity uplynul dvojnasobek DOBA_ZAMCENI -
 * v tu chvili uz nemaji na vysledek jeZamceno zadny vliv, takze je bezpecne je zahodit.
 */
@Component
public class PrihlaseniOmezovac {

    private static final int MAX_NEUSPESNYCH_POKUSU = 5;
    private static final Duration DOBA_ZAMCENI = Duration.ofMinutes(15);

    private final Clock hodiny;
    private final Map<String, Zaznam> zaznamy = new ConcurrentHashMap<>();

    public PrihlaseniOmezovac(Clock hodiny) {
        this.hodiny = hodiny;
    }

    public boolean jeZamceno(String email) {
        Zaznam zaznam = zaznamy.get(EmailValidace.normalizuj(email));
        return zaznam != null && zaznam.zamcenoDo != null && Instant.now(hodiny).isBefore(zaznam.zamcenoDo);
    }

    public void zaznamenejNeuspech(String email) {
        Instant ted = Instant.now(hodiny);
        zaznamy.compute(EmailValidace.normalizuj(email), (klic, zaznam) -> {
            Zaznam aktualni = zaznam == null ? new Zaznam() : zaznam;
            aktualni.pocetNeuspechu++;
            aktualni.posledniAktivita = ted;
            if (aktualni.pocetNeuspechu >= MAX_NEUSPESNYCH_POKUSU) {
                aktualni.zamcenoDo = ted.plus(DOBA_ZAMCENI);
                aktualni.pocetNeuspechu = 0;
            }
            return aktualni;
        });
        uklidStarychZaznamu(ted);
    }

    public void zaznamenejUspech(String email) {
        zaznamy.remove(EmailValidace.normalizuj(email));
    }

    private void uklidStarychZaznamu(Instant ted) {
        Instant hranice = ted.minus(DOBA_ZAMCENI.multipliedBy(2));
        zaznamy.values().removeIf(zaznam -> zaznam.posledniAktivita.isBefore(hranice));
    }

    /** Jen pro testy - overeni, ze mapa opravdu neroste neomezene. */
    int pocetZaznamu() {
        return zaznamy.size();
    }

    private static final class Zaznam {
        private int pocetNeuspechu;
        private Instant zamcenoDo;
        private Instant posledniAktivita;
    }
}
