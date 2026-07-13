package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Role;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MojeJmenoServiceTest {

    private UzivatelRepository uzivatelRepository;
    private PasswordEncoder passwordEncoder;
    private MojeJmenoService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new MojeJmenoService(uzivatelRepository, passwordEncoder, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Uzivatel uzivatel() {
        return new Uzivatel("admin", "$2a$stareHash", Role.ADMIN);
    }

    @Test
    void zmenJmenoUlozNoveJmenoKdyzHesloSouhlasiAJmenoJeVolne() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);

        String vysledek = service.zmenJmeno("admin", "heslo", "novak");

        assertThat(vysledek).isEqualTo("novak");
        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getJmeno()).isEqualTo("novak");
    }

    @Test
    void zmenJmenoOriznePrebytecneMezery() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);

        service.zmenJmeno("admin", "heslo", "  novak  ");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getJmeno()).isEqualTo("novak");
    }

    @Test
    void zmenJmenoSNespravnymHeslemVyhodiChybu() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("spatneheslo", "$2a$stareHash")).willReturn(false);

        assertThatThrownBy(() -> service.zmenJmeno("admin", "spatneheslo", "novak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nesouhlasí");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenJmenoSPrazdnymNovymJmenemVyhodiChybu() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);

        assertThatThrownBy(() -> service.zmenJmeno("admin", "heslo", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("povinné");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenJmenoSObsazenymJmenemVyhodiChybu() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByJmeno("asistentka")).willReturn(true);

        assertThatThrownBy(() -> service.zmenJmeno("admin", "heslo", "asistentka"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obsazené");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenJmenoNaStejneJmenoNepovazujeSeZaObsazene() {
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);

        service.zmenJmeno("admin", "heslo", "admin");

        verify(uzivatelRepository, never()).existsByJmeno(any());
        verify(uzivatelRepository).save(any());
    }
}
