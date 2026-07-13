package cz.petrk.dokgen.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Jednoducha kontrola formatu emailu - staci pro overeni pri registraci a zmene emailu. */
public final class EmailValidace {

    private static final Pattern VZOR = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailValidace() {
    }

    public static boolean jePlatny(String email) {
        return email != null && VZOR.matcher(email).matches();
    }

    /**
     * Sjednocene orezani a prevod na mala pismena - pouzij VSUDE, kde se s emailem
     * pracuje (vyhledani uctu, registrace, zmena emailu...), aby "Novak@Example.com"
     * a "novak@example.com" byl vzdy tentyz ucet. Bez tohohle by slo zalozit dva
     * ucty lisici se jen velikosti pismen a prihlaseni s jinak napsanym emailem
     * by selhalo.
     */
    public static String normalizuj(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
