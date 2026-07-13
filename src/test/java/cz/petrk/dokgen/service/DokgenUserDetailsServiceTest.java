package cz.petrk.dokgen.service;

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
import static org.mockito.Mockito.verify;

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
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(new Uzivatel("admin@dokgen.local", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin@dokgen.local")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("admin@dokgen.local");

        assertThat(detail.getUsername()).isEqualTo("admin@dokgen.local");
        assertThat(detail.getPassword()).isEqualTo("$2a$hash");
        assertThat(detail.isAccountNonLocked()).isTrue();
    }

    @Test
    void aktivniUcetJeVracenJakoPovoleny() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(new Uzivatel("admin@dokgen.local", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin@dokgen.local")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("admin@dokgen.local");

        assertThat(detail.isEnabled()).isTrue();
    }

    @Test
    void neschvalenyUcetJeVracenJakoZakazany() {
        given(uzivatelRepository.findByEmail("novak@example.com")).willReturn(Optional.of(new Uzivatel("novak@example.com", "$2a$hash", false)));
        given(prihlaseniOmezovac.jeZamceno("novak@example.com")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("novak@example.com");

        assertThat(detail.isEnabled()).isFalse();
    }

    @Test
    void ucetZamcenyKvuliOpakovanymNeuspechumSePromitneDoUserDetails() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(new Uzivatel("admin@dokgen.local", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin@dokgen.local")).willReturn(true);

        UserDetails detail = service.loadUserByUsername("admin@dokgen.local");

        assertThat(detail.isAccountNonLocked()).isFalse();
    }

    @Test
    void prihlaseniSJinouVelikostiPismenNajdeStejnyUcet() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(new Uzivatel("admin@dokgen.local", "$2a$hash")));
        given(prihlaseniOmezovac.jeZamceno("admin@dokgen.local")).willReturn(false);

        UserDetails detail = service.loadUserByUsername("  Admin@Dokgen.Local  ");

        assertThat(detail.getUsername()).isEqualTo("admin@dokgen.local");
        verify(prihlaseniOmezovac).jeZamceno("admin@dokgen.local");
    }

    @Test
    void neexistujiciUzivatelVyhodiChybu() {
        given(uzivatelRepository.findByEmail("neznamy@dokgen.local")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("neznamy@dokgen.local"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
