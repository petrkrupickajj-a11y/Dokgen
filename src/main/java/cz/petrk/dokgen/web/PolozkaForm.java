package cz.petrk.dokgen.web;

import java.math.BigDecimal;

/** Jeden radek formulare polozek na /generovat/{id} - nazev, mnozstvi a cena za jednotku, jak je zadal uzivatel. */
public class PolozkaForm {

    private String nazev;
    private BigDecimal mnozstvi;
    private BigDecimal cena;

    public String getNazev() {
        return nazev;
    }

    public void setNazev(String nazev) {
        this.nazev = nazev;
    }

    public BigDecimal getMnozstvi() {
        return mnozstvi;
    }

    public void setMnozstvi(BigDecimal mnozstvi) {
        this.mnozstvi = mnozstvi;
    }

    public BigDecimal getCena() {
        return cena;
    }

    public void setCena(BigDecimal cena) {
        this.cena = cena;
    }
}
