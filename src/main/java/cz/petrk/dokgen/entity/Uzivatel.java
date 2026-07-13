package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/**
 * Prihlasovaci ucet appky. Heslo se uklada jen jako BCrypt hash (nikdy
 * v plain textu) - viz DokgenUserDetailsService a RegistraceService.
 *
 * Role urcuje opravneni (viz SecurityConfig) - dvouargumentovy konstruktor
 * bez role existuje pro zpetnou kompatibilitu (napr. ZmenaHeslaRunner) a
 * dava uctu roli ADMIN, aby appka nikdy nevytvorila ucet, ktery by se
 * nedostal ani ke sprave sablon/uctu.
 */
@Entity
public class Uzivatel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String jmeno;

    private String heslo;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime vytvorenoDne;

    protected Uzivatel() {
    }

    public Uzivatel(String jmeno, String heslo) {
        this(jmeno, heslo, Role.ADMIN);
    }

    public Uzivatel(String jmeno, String heslo, Role role) {
        this.jmeno = jmeno;
        this.heslo = heslo;
        this.role = role;
        this.vytvorenoDne = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getJmeno() {
        return jmeno;
    }

    public String getHeslo() {
        return heslo;
    }

    public void setHeslo(String heslo) {
        this.heslo = heslo;
    }

    public Role getRole() {
        return role;
    }

    /** Pro doplneni role uctum vytvorenym pred zavedenim roli (viz UzivateleSeeder). */
    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getVytvorenoDne() {
        return vytvorenoDne;
    }
}
