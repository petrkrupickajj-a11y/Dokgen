package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Jednorazova normalizace emailu existujicich uctu (mala pismena, bez
 * okrajovych mezer, viz EmailValidace.normalizuj) - appka od zavedeni
 * teto normalizace porovnava emaily uz jen v normalizovane podobe (viz
 * RegistraceService, DokgenUserDetailsService, MojeEmailService,
 * ResetHeslaService, PrihlaseniOmezovac), takze ucty zalozene pred touto
 * zmenou by jinak nemusely jit spravne najit (napr. prihlaseni s jinak
 * napsanymi velkymi/malymi pismeny by selhalo).
 *
 * Kdyby normalizace dvou ruznych uctu vedla ke stejnemu emailu (kolize,
 * napr. "Novak@example.com" a "novak@example.com" zalozene pred zavedenim
 * tehle kontroly), appka radeji nic nesmaze ani neprepise - jen to
 * zaloguje jako varovani, at si to nekdo rucne overi a rozhodne, ktery
 * ucet ma zustat.
 */
@Component
public class EmailNormalizaceRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNormalizaceRunner.class);

    private final UzivatelRepository uzivatelRepository;

    public EmailNormalizaceRunner(UzivatelRepository uzivatelRepository) {
        this.uzivatelRepository = uzivatelRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Uzivatel> vsichni = uzivatelRepository.findAll();
        Map<String, List<Uzivatel>> podleNormalizovanehoEmailu = vsichni.stream()
                .collect(Collectors.groupingBy(u -> EmailValidace.normalizuj(u.getEmail())));

        for (Uzivatel uzivatel : vsichni) {
            String normalizovany = EmailValidace.normalizuj(uzivatel.getEmail());
            if (normalizovany.equals(uzivatel.getEmail())) {
                continue;
            }

            List<Uzivatel> kolidujici = podleNormalizovanehoEmailu.get(normalizovany);
            if (kolidujici.size() > 1) {
                LOG.warn("Účty s id {} by po normalizaci emailu měly stejný email \"{}\" - ponechávám beze změny, over prosím ručně.",
                        kolidujici.stream().map(Uzivatel::getId).toList(), normalizovany);
                continue;
            }

            uzivatel.setEmail(normalizovany);
            uzivatelRepository.save(uzivatel);
        }
    }
}
