package cz.petrk.dokgen.util;

import cz.petrk.dokgen.config.DodavatelProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** Prevod DodavatelProperties na obecnou mapu placeholderu pro DocumentGeneratorService - viz KlientData, stejny princip. */
public final class DodavatelData {

    private DodavatelData() {
    }

    public static Map<String, String> sestavKontext(DodavatelProperties dodavatel) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("dodavatel.nazev", nullSafe(dodavatel.getNazev()));
        data.put("dodavatel.sidlo", nullSafe(dodavatel.getSidlo()));
        data.put("dodavatel.ico", nullSafe(dodavatel.getIco()));
        data.put("dodavatel.cisloUctu", nullSafe(dodavatel.getCisloUctu()));
        return data;
    }

    private static String nullSafe(String hodnota) {
        return hodnota == null ? "" : hodnota;
    }
}
