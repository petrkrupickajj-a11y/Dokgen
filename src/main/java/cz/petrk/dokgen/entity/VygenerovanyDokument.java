package cz.petrk.dokgen.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Zaznam o jednom vygenerovanem dokumentu (audit log). Jmeno klienta a nazev
 * sablony se ukladaji jako snapshot v okamziku generovani, aby historie
 * zustala citelna i kdyz klienta nebo sablonu nekdo pozdeji smaze.
 *
 * Samotny vygenerovany soubor se neuklada - jen zaznam ze k tomu doslo.
 */
@Entity
public class VygenerovanyDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long klientId;

    private String klientJmenoPrijmeni;

    private String sablonaNazev;

    private String format;

    private LocalDateTime vytvorenoDne;

    protected VygenerovanyDokument() {
    }

    public VygenerovanyDokument(Long klientId, String klientJmenoPrijmeni, String sablonaNazev, String format) {
        this.klientId = klientId;
        this.klientJmenoPrijmeni = klientJmenoPrijmeni;
        this.sablonaNazev = sablonaNazev;
        this.format = format;
        this.vytvorenoDne = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getKlientId() {
        return klientId;
    }

    public String getKlientJmenoPrijmeni() {
        return klientJmenoPrijmeni;
    }

    public String getSablonaNazev() {
        return sablonaNazev;
    }

    public String getFormat() {
        return format;
    }

    public LocalDateTime getVytvorenoDne() {
        return vytvorenoDne;
    }
}
