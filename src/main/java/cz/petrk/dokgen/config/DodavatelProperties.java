package cz.petrk.dokgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Udaje dodavatele (nasi firmy) pro fakturu - viz application.properties,
 * dokgen.dodavatel.*. Stranka /nastaveni dnes resi jen prihlasovaci email
 * (NastaveniController), zadnou obecnou perzistenci klic-hodnota nema, proto
 * jsou tyhle udaje - stejne jako prihlasovaci ucty v UzivateleProperties -
 * v konfiguraci, ne v databazi.
 */
@ConfigurationProperties(prefix = "dokgen.dodavatel")
public class DodavatelProperties {

    private String nazev;
    private String sidlo;
    private String ico;
    private String cisloUctu;

    public String getNazev() {
        return nazev;
    }

    public void setNazev(String nazev) {
        this.nazev = nazev;
    }

    public String getSidlo() {
        return sidlo;
    }

    public void setSidlo(String sidlo) {
        this.sidlo = sidlo;
    }

    public String getIco() {
        return ico;
    }

    public void setIco(String ico) {
        this.ico = ico;
    }

    public String getCisloUctu() {
        return cisloUctu;
    }

    public void setCisloUctu(String cisloUctu) {
        this.cisloUctu = cisloUctu;
    }
}
