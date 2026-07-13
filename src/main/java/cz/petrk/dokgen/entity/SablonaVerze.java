package cz.petrk.dokgen.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Snapshot obsahu sablony pred jejim prepsanim (tlacitko "Nahradit" na
 * /sablony, nebo pred obnovenim jine starsi verze). Samotny .docx obsah
 * verze se uklada na disk stejnym zpusobem jako aktualni soubor sablony
 * (viz SablonaUlozisteService) - tady drzime jen odkaz na nej a kdy vznikl.
 */
@Entity
public class SablonaVerze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sablonaId;

    private String nazevSouboru;

    private LocalDateTime ulozenoDne;

    protected SablonaVerze() {
    }

    public SablonaVerze(Long sablonaId, String nazevSouboru) {
        this.sablonaId = sablonaId;
        this.nazevSouboru = nazevSouboru;
        this.ulozenoDne = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSablonaId() {
        return sablonaId;
    }

    public String getNazevSouboru() {
        return nazevSouboru;
    }

    public LocalDateTime getUlozenoDne() {
        return ulozenoDne;
    }
}
