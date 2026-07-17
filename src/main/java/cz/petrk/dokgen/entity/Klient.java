package cz.petrk.dokgen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Jeden zaznam v databazi = jeden klient / jedna sada udaju,
 * ktere pak muzeme naklikat do libovolne Word sablony.
 *
 * Kdyz budes potrebovat dalsi pole (napr. DIC, cislo bankovniho uctu...),
 * staci ho sem pridat + pridat radek do formulare (formular.html)
 * a do DocumentGeneratorService.sestavData().
 */
@Entity
public class Klient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{klient.jmeno.povinne}")
    @Size(max = 100, message = "{klient.jmeno.dlouhe}")
    @Column(nullable = false, length = 100)
    private String jmeno;

    @NotBlank(message = "{klient.prijmeni.povinne}")
    @Size(max = 100, message = "{klient.prijmeni.dlouhe}")
    @Column(nullable = false, length = 100)
    private String prijmeni;

    @Pattern(regexp = "^$|^(\\+420 ?|00420 ?)?[0-9]{3} ?[0-9]{3} ?[0-9]{3}$",
            message = "{klient.telefon.format}")
    private String telefon;

    @Email(message = "{klient.email.format}")
    private String email;

    // Vychozi delka VARCHAR sloupce v Hibernate je 255 - bez explicitniho
    // @Column(length=...) by @Size(max=...) na urovni validace a skutecny
    // limit v databazi nesouhlasily (adresa/poznamka pripousti vic nez 255 znaku).
    @Size(max = 200, message = "{klient.adresa.dlouha}")
    @Column(length = 200)
    private String adresa;

    @Size(max = 100, message = "{klient.mesto.dlouhe}")
    @Column(length = 100)
    private String mesto;

    @Pattern(regexp = "^$|^\\d{3} ?\\d{2}$", message = "{klient.psc.format}")
    private String psc;

    @Pattern(regexp = "^$|^\\d{8}$", message = "{klient.ico.format}")
    private String ico;

    @Size(max = 1000, message = "{klient.poznamka.dlouha}")
    @Column(length = 1000)
    private String poznamka;

    public Klient() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJmeno() {
        return jmeno;
    }

    public void setJmeno(String jmeno) {
        this.jmeno = jmeno;
    }

    public String getPrijmeni() {
        return prijmeni;
    }

    public void setPrijmeni(String prijmeni) {
        this.prijmeni = prijmeni;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getMesto() {
        return mesto;
    }

    public void setMesto(String mesto) {
        this.mesto = mesto;
    }

    public String getPsc() {
        return psc;
    }

    public void setPsc(String psc) {
        this.psc = psc;
    }

    public String getIco() {
        return ico;
    }

    public void setIco(String ico) {
        this.ico = ico;
    }

    public String getPoznamka() {
        return poznamka;
    }

    public void setPoznamka(String poznamka) {
        this.poznamka = poznamka;
    }
}
