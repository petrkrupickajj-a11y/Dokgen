package cz.petrk.dokgen.web;

import java.util.ArrayList;
import java.util.List;

/**
 * Faktura-specificke udaje z formulare na /generovat/{id} - zobrazuji se jen
 * u sablon s polozkami (viz DocumentGeneratorService.sablonaObsahujePolozky),
 * u ostatnich sablon zustanou vsechna pole na vychozich hodnotach.
 */
public class FakturaForm {

    private String cisloFaktury;
    private Integer splatnostDny = 14;
    private List<PolozkaForm> polozky = new ArrayList<>();

    public String getCisloFaktury() {
        return cisloFaktury;
    }

    public void setCisloFaktury(String cisloFaktury) {
        this.cisloFaktury = cisloFaktury;
    }

    public Integer getSplatnostDny() {
        return splatnostDny;
    }

    public void setSplatnostDny(Integer splatnostDny) {
        this.splatnostDny = splatnostDny;
    }

    public List<PolozkaForm> getPolozky() {
        return polozky;
    }

    public void setPolozky(List<PolozkaForm> polozky) {
        this.polozky = polozky;
    }
}
