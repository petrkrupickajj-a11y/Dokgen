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
 *
 * Novy ucet z verejne registrace ceka na schvaleni (aktivni=false) - dvou-
 * argumentovy konstruktor pouzivaji jen dovery-hodne cesty (UzivateleSeeder,
 * konzolovy ZmenaHeslaRunner), takze rovnou zaklada aktivni ucet. "aktivni"
 * je Boolean (ne primitivni boolean) schvalne - u uctu zalozenych pred
 * zavedenim schvalovani zustane sloupec po Hibernate ddl-auto=update prazdny
 * (null), a UzivateleSeeder je pri startu doplni na true (viz
 * doplnChybejiciAktivni) - primitivni boolean by pri mapovani NULL hodnoty
 * spadl. jeAktivni() proto navic bere null jako "neaktivni" (bezpecnejsi
 * vychozi chovani, kdyby backfill z nejakeho duvodu jeste neprobehl).
 */
@Entity
public class Uzivatel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String heslo;

    private Boolean aktivni;

    private LocalDateTime vytvorenoDne;

    protected Uzivatel() {
    }

    public Uzivatel(String email, String heslo) {
        this(email, heslo, true);
    }

    public Uzivatel(String email, String heslo, boolean aktivni) {
        this.email = email;
        this.heslo = heslo;
        this.aktivni = aktivni;
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

    /** Null-safe cteni (viz komentar u tridy) - null se bere jako neaktivni. */
    public boolean jeAktivni() {
        return Boolean.TRUE.equals(aktivni);
    }

    /** Pro schvalovani/backfill starsich uctu (viz SpravaUctuService, UzivateleSeeder). */
    public Boolean getAktivni() {
        return aktivni;
    }

    public void setAktivni(Boolean aktivni) {
        this.aktivni = aktivni;
    }

    public LocalDateTime getVytvorenoDne() {
        return vytvorenoDne;
    }
}
