package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * "Tombstone" zaznam - drzi si jmeno vestavene sablony, kterou uzivatel
 * umyslne smazal. Bez tohohle by SablonySeeder pri kazdem startu appky
 * smazanou vestavenou sablonu znovu obnovil ze zabudoveneho zdroje.
 */
@Entity
public class SmazanaVestavenaSablona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nazev;

    protected SmazanaVestavenaSablona() {
    }

    public SmazanaVestavenaSablona(String nazev) {
        this.nazev = nazev;
    }

    public Long getId() {
        return id;
    }

    public String getNazev() {
        return nazev;
    }
}
