package cz.petrk.dokgen.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jednoducha ochrana proti hromadnemu zakladani uctu (/registrace) a
 * spamovani zadosti o reset hesla (/zapomenute-heslo) z jedne IP adresy -
 * stejna filozofie jako PrihlaseniOmezovac (in-memory, bez externiho
 * uloziste), jen misto poctu neuspesnych prihlaseni na email pocita
 * pozadavky na IP adresu v pevnem casovem okne.
 *
 * Kazda IP adresa smi za OKNO poslat nejvyse MAX_POZADAVKU pozadavku -
 * pri prekroceni appka ukaze srozumitelnou hlasku a pozadavek neprovede.
 * Okno zacina prvnim pozadavkem dane IP adresy a po jeho uplynuti se
 * pocitadlo samo vynuluje.
 *
 * Zaznamy zijou jen v pameti procesu - restart appky je vynuluje. Pro
 * appku pouzivanou jednotkami lidi v ramci jedne firmy je to dostatecna
 * ochrana bez potreby externiho uloziste (Redis apod.).
 *
 * Mapa by jinak rostla neomezene - kazda IP adresa, ktera kdy poslala
 * pozadavek, by v ni navzdy zustala. povolPozadavek proto pri kazdem
 * zapisu promaze zaznamy s jiz uplynulym oknem (stejny princip jako
 * PrihlaseniOmezovac.uklidStarychZaznamu).
 */
@Component
public class IpOmezovac {

    private static final int MAX_POZADAVKU = 5;
    private static final Duration OKNO = Duration.ofMinutes(15);

    private final Clock hodiny;
    private final Map<String, Zaznam> zaznamy = new ConcurrentHashMap<>();

    public IpOmezovac(Clock hodiny) {
        this.hodiny = hodiny;
    }

    /** Zapocita pozadavek z dane IP adresy a vrati true, pokud jeste smi projit. */
    public boolean povolPozadavek(String ip) {
        String klic = ip == null ? "" : ip;
        Instant ted = Instant.now(hodiny);

        Zaznam zaznam = zaznamy.compute(klic, (k, aktualni) -> {
            if (aktualni == null || ted.isAfter(aktualni.oknoKonci)) {
                Zaznam novy = new Zaznam();
                novy.oknoKonci = ted.plus(OKNO);
                novy.pocet = 1;
                return novy;
            }
            aktualni.pocet++;
            return aktualni;
        });

        uklidStarychZaznamu(ted);
        return zaznam.pocet <= MAX_POZADAVKU;
    }

    private void uklidStarychZaznamu(Instant ted) {
        zaznamy.values().removeIf(zaznam -> ted.isAfter(zaznam.oknoKonci));
    }

    /** Jen pro testy - overeni, ze mapa opravdu neroste neomezene. */
    int pocetZaznamu() {
        return zaznamy.size();
    }

    private static final class Zaznam {
        private int pocet;
        private Instant oknoKonci;
    }
}
