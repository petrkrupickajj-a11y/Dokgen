package cz.petrk.dokgen.web;

/**
 * Neplatna hodnota v pozadavku, kterou uzivatel bezne cestou appky poslat
 * nemuze (napr. jiny format dokumentu nez WORD/PDF) - na rozdil od
 * IllegalArgumentException pouzivane pro "neexistujici zaznam" (404, viz
 * GlobalExceptionHandler) se tahle mapuje na 400 Bad Request.
 */
public class NeplatnyVstupException extends RuntimeException {

    public NeplatnyVstupException(String zprava) {
        super(zprava);
    }
}
