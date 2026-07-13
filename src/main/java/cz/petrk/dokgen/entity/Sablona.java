package cz.petrk.dokgen.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Jedna dostupna sablona pro generovani dokumentu.
 *
 * "Vestavena" sablona je jedna z peti puvodnich sablon dodanych s appkou
 * (nejde smazat pres UI). Ostatni jsou nahrane uzivatelem pres /sablony.
 *
 * Samotny obsah .docx souboru se neuklada do databaze, jen jeho nazev -
 * skutecny soubor lezi v adresari spravovanem SablonaUlozisteService.
 */
@Entity
public class Sablona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nazev;

    private String nazevSouboru;

    private boolean vestavena;

    private LocalDateTime nahranoDne;

    protected Sablona() {
    }

    public Sablona(String nazev, String nazevSouboru, boolean vestavena) {
        this.nazev = nazev;
        this.nazevSouboru = nazevSouboru;
        this.vestavena = vestavena;
        this.nahranoDne = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNazev() {
        return nazev;
    }

    public String getNazevSouboru() {
        return nazevSouboru;
    }

    public boolean isVestavena() {
        return vestavena;
    }

    public LocalDateTime getNahranoDne() {
        return nahranoDne;
    }

    /** Zavola se po nahrazeni obsahu souboru sablony (uprava mimo appku). */
    public void oznacUpraveno() {
        this.nahranoDne = LocalDateTime.now();
    }
}
