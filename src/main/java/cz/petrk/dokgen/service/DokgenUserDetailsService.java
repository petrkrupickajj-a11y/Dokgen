package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Prihlasovaci ucty appky se nactou z databaze (entita Uzivatel) - jednak
 * ty vestavene (viz UzivateleSeeder, ktery je pri prvnim startu naplni
 * z application.properties), jednak nove pridane pres /registrace. Kazdy
 * ucet ma stejna opravneni, zadne role se nerozlisuji.
 *
 * Kazdy ucet se navic ptá PrihlaseniOmezovac, jestli neni docasne zamceny
 * kvuli opakovanym neuspesnym pokusum - Spring Security pak samo odmitne
 * prihlaseni jeste pred kontrolou hesla.
 */
@Service
public class DokgenUserDetailsService implements UserDetailsService {

    private final UzivatelRepository uzivatelRepository;
    private final PrihlaseniOmezovac prihlaseniOmezovac;

    public DokgenUserDetailsService(UzivatelRepository uzivatelRepository, PrihlaseniOmezovac prihlaseniOmezovac) {
        this.uzivatelRepository = uzivatelRepository;
        this.prihlaseniOmezovac = prihlaseniOmezovac;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Uzivatel uzivatel = uzivatelRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel \"" + email + "\" neexistuje"));
        return User.withUsername(uzivatel.getEmail())
                .password(uzivatel.getHeslo())
                .authorities(Collections.emptyList())
                .accountLocked(prihlaseniOmezovac.jeZamceno(email))
                .build();
    }
}
