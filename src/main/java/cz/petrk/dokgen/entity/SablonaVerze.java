package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Snapshot obsahu sablony pred jejim prepsanim (tlacitko "Nahradit" na
 * /sablony, nebo pred obnovenim jine starsi verze). Samotny .docx obsah
 * verze se uklada na disk stejnym zpusobem jako aktualni soubor sablony
 * (viz SablonaUlozisteService) - tady drzime jen odkaz na nej a kdy vznikl.
 *
 * Index na (sablonaId, ulozenoDne) odpovida presne dotazu
 * SablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc - bez nej by
 * appka s pribyvajicimi verzemi sablon musela pri kazdem zobrazeni historie
 * verzi delat full table scan.
 */
@Entity
@Table(indexes = @Index(name = "idx_sablona_verze_sablona_id", columnList = "sablonaId, ulozenoDne"))
public class SablonaVerze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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
