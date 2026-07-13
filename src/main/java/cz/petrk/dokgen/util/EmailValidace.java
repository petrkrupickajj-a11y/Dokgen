package cz.petrk.dokgen.util;

import java.util.regex.Pattern;

/** Jednoducha kontrola formatu emailu - staci pro overeni pri registraci a zmene emailu. */
public final class EmailValidace {

    private static final Pattern VZOR = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailValidace() {
    }

    public static boolean jePlatny(String email) {
        return email != null && VZOR.matcher(email).matches();
    }
}
