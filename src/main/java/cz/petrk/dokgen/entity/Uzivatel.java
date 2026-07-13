package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Prihlasovaci ucet appky. Prihlasuje se emailem (viz DokgenUserDetailsService,
 * SecurityConfig) - heslo se uklada jen jako BCrypt hash (nikdy v plain textu).
 * Kazdy prihlaseny ucet ma stejna opravneni, zadne role se nerozlisuji.
 */
@Entity
public class Uzivatel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String heslo;

    private LocalDateTime vytvorenoDne;

    protected Uzivatel() {
    }

    public Uzivatel(String email, String heslo) {
        this.email = email;
        this.heslo = heslo;
        this.vytvorenoDne = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    /** Pro samoobslusnou zmenu emailu (viz MojeEmailService). */
    public void setEmail(String email) {
        this.email = email;
    }

    public String getHeslo() {
        return heslo;
    }

    public void setHeslo(String heslo) {
        this.heslo = heslo;
    }

    public LocalDateTime getVytvorenoDne() {
        return vytvorenoDne;
    }
}
