package cz.petrk.dokgen.util;

import cz.petrk.dokgen.entity.Klient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prevod entity Klient na obecnou mapu placeholderu pro DocumentGeneratorService.
 * Zajmena existuje proto, aby DocumentGeneratorService nemusel o entite Klient
 * vubec vedet - generator pracuje jen s Map<String,String>, prevod domenoveho
 * objektu na tuhle mapu je vec volajiciho (KlientController).
 */
public final class KlientData {

    private KlientData() {
    }

    public static Map<String, String> sestavKontext(Klient klient) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("jmeno", nullSafe(klient.getJmeno()));
        data.put("prijmeni", nullSafe(klient.getPrijmeni()));
        data.put("telefon", nullSafe(klient.getTelefon()));
        data.put("email", nullSafe(klient.getEmail()));
        data.put("adresa", nullSafe(klient.getAdresa()));
        data.put("mesto", nullSafe(klient.getMesto()));
        data.put("psc", nullSafe(klient.getPsc()));
        data.put("ico", nullSafe(klient.getIco()));
        data.put("poznamka", nullSafe(klient.getPoznamka()));
        return data;
    }

    private static String nullSafe(String hodnota) {
        return hodnota == null ? "" : hodnota;
    }
}
