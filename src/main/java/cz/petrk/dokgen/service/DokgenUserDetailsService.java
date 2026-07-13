package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Prihlasovaci ucty appky se nactou z databaze (entita Uzivatel) - jednak
 * ty vestavene (viz UzivateleSeeder, ktery je pri prvnim startu naplni
 * z application.properties), jednak nove pridane pres /registrace. Role
 * uctu (ADMIN/ASISTENTKA) se preda Spring Security jako autorita ROLE_xxx -
 * SecurityConfig podle ni omezuje pristup na /sablony a /registrace.
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
    public UserDetails loadUserByUsername(String jmeno) throws UsernameNotFoundException {
        Uzivatel uzivatel = uzivatelRepository.findByJmeno(jmeno)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel \"" + jmeno + "\" neexistuje"));
        return User.withUsername(uzivatel.getJmeno())
                .password(uzivatel.getHeslo())
                .roles(uzivatel.getRole().name())
                .accountLocked(prihlaseniOmezovac.jeZamceno(jmeno))
                .build();
    }
}
