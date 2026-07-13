package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Role;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class DokgenUserDetailsServiceTest {

    private UzivatelRepository uzivatelRepository;
    private PrihlaseniOmezovac prihlaseniOmezovac;
    private DokgenUserDetailsService service;

    @BeforeEach
    void setUp() {
        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        prihlaseniOmezovac = Mockito.mock(PrihlaseniOmezovac.class);
        service = new DokgenUserDetailsService(uzivatelRepository, prihlaseniOmezovac);
    }

    @Test
    void nezamcenyUcetJeVracenAJakoOdemceny() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(new Uzivatel("admin", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("admin");

        assertThat(detail.getUsername()).isEqualTo("admin");
        assertThat(detail.getPassword()).isEqualTo("$2a$hash");
        assertThat(detail.isAccountNonLocked()).isTrue();
    }

    @Test
    void ucetZamcenyKvuliOpakovanymNeuspechumSePromitneDoUserDetails() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(new Uzivatel("admin", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin")).willReturn(true);

        UserDetails detail = service.loadUserByUsername("admin");

        assertThat(detail.isAccountNonLocked()).isFalse();
    }

    @Test
    void roleUctuSePromitneDoAutorityRoleAdmin() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(new Uzivatel("admin", "$2a$hash", Role.ADMIN)));
        given(prihlaseniOmezovac.jeZamceno("admin")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("admin");

        assertThat(detail.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void roleUctuSePromitneDoAutorityRoleAsistentka() {
        given(uzivatelRepository.findByJmeno("asistentka")).willReturn(Optional.of(new Uzivatel("asistentka", "$2a$hash", Role.ASISTENTKA)));
        given(prihlaseniOmezovac.jeZamceno("asistentka")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("asistentka");

        assertThat(detail.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ASISTENTKA");
    }

    @Test
    void neexistujiciUzivatelVyhodiChybu() {
        given(uzivatelRepository.findByJmeno("neznamy")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("neznamy"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
