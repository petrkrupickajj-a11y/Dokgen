package cz.petrk.dokgen.service;

import cz.petrk.dokgen.config.UzivateleProperties;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pri prvnim startu appky zajisti, ze vychozi ucty z application.properties
 * (dokgen.uzivatele) existuji v databazi. Pote uz appka bere prihlasovaci
 * udaje vyhradne z databaze (viz DokgenUserDetailsService) - zmena hesla
 * v properties po prvnim startu uz nic neprepise, aby se necekane
 * nepremazalo heslo, ktere si nekdo mezitim zmenil.
 *
 * Pokud pro ucet neni v application.properties (resp. v prislusne promenne
 * prostredi DOKGEN_HESLO / DOKGEN_HESLO_ASISTENTKA) nastavene zadne heslo,
 * appka mu misto pevneho/uhodnutelneho hesla vygeneruje nahodne jednorazove
 * heslo a jednou ho vypise do logu - stejny princip jako vestaveny
 * "Using generated security password" u Spring Security, jen na urovni
 * nasich vlastnich uctu.
 *
 * Zaroven pri kazdem startu doplni priznak "aktivni" uctum, ktere v databazi
 * existuji z doby pred zavedenim schvalovani novych uctu (sloupec u nich
 * zustal po Hibernate ddl-auto=update prazdny/null) - dostanou true, aby je
 * zavedeni schvalovani nikoho nevymklo z appky, kterou uz pouziva.
 */
@Component
public class UzivateleSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(UzivateleSeeder.class);

    private final UzivateleProperties uzivateleProperties;
    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;

    public UzivateleSeeder(UzivateleProperties uzivateleProperties,
                            UzivatelRepository uzivatelRepository,
                            PasswordEncoder passwordEncoder) {
        this.uzivateleProperties = uzivateleProperties;
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        doplnChybejiciAktivni();

        for (UzivateleProperties.Ucet ucet : uzivateleProperties.getUzivatele()) {
            if (uzivatelRepository.existsByEmail(ucet.getEmail())) {
                continue;
            }

            String heslo = ucet.getHeslo();
            if (heslo == null || heslo.isBlank()) {
                heslo = UUID.randomUUID().toString();
                LOG.warn("""


                        Pro účet "{}" není v application.properties (ani v odpovídající proměnné
                        prostředí) nastavené heslo - appka mu vygenerovala toto NÁHODNÉ jednorázové heslo:

                            {}

                        Poznač si ho hned teď, appka ho už znovu nevypíše. Appka od teď bere přihlašovací
                        údaje výhradně z databáze - úprava application.properties už nic nezmění.
                        """, ucet.getEmail(), heslo);
            }

            uzivatelRepository.save(new Uzivatel(ucet.getEmail(), passwordEncoder.encode(heslo)));
        }
    }

    private void doplnChybejiciAktivni() {
        for (Uzivatel uzivatel : uzivatelRepository.findAll()) {
            if (uzivatel.getAktivni() == null) {
                uzivatel.setAktivni(true);
                uzivatelRepository.save(uzivatel);
            }
        }
    }
}
