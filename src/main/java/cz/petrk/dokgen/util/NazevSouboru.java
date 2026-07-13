package cz.petrk.dokgen.util;

import java.text.Normalizer;
import java.util.Locale;

/** Odstrani diakritiku a mezery, aby nazev stahovaneho souboru byl bezpecny. */
public final class NazevSouboru {

    private NazevSouboru() {
    }

    public static String ocisti(String vstup) {
        if (vstup == null) {
            return "dokument";
        }
        String bezDiakritiky = Normalizer.normalize(vstup, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return bezDiakritiky.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase(Locale.ROOT);
    }
}
