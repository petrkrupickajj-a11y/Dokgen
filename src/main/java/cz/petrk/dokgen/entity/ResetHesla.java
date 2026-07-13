package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/**
 * Jednorazovy token pro reset zapomenuteho hesla (viz ResetHeslaService).
 * Uklada se jen hash tokenu (SHA-256), ne token samotny - stejny princip
 * jako u hesel (Uzivatel.heslo), aby unik databaze sam o sobe neumoznil
 * token pouzit. Token ma casove omezenou platnost a jde pouzit jen jednou.
 */
@Entity
public class ResetHesla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Uzivatel uzivatel;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    private LocalDateTime vyprsiDne;

    private boolean pouzit;

    protected ResetHesla() {
    }

    public ResetHesla(Uzivatel uzivatel, String tokenHash, LocalDateTime vyprsiDne) {
        this.uzivatel = uzivatel;
        this.tokenHash = tokenHash;
        this.vyprsiDne = vyprsiDne;
        this.pouzit = false;
    }

    public Long getId() {
        return id;
    }

    public Uzivatel getUzivatel() {
        return uzivatel;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getVyprsiDne() {
        return vyprsiDne;
    }

    public boolean isPouzit() {
        return pouzit;
    }

    public void setPouzit(boolean pouzit) {
        this.pouzit = pouzit;
    }
}
