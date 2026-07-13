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

class MojeEmailServiceTest {

    private UzivatelRepository uzivatelRepository;
    private PasswordEncoder passwordEncoder;
    private MojeEmailService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new MojeEmailService(uzivatelRepository, passwordEncoder, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Uzivatel uzivatel() {
        return new Uzivatel("admin@dokgen.local", "$2a$stareHash", Role.ADMIN);
    }

    @Test
    void zmenEmailUlozNovyEmailKdyzHesloSouhlasiAEmailJeVolny() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByEmail("novy@example.com")).willReturn(false);

        String vysledek = service.zmenEmail("admin@dokgen.local", "heslo", "novy@example.com");

        assertThat(vysledek).isEqualTo("novy@example.com");
        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getEmail()).isEqualTo("novy@example.com");
    }

    @Test
    void zmenEmailOriznePrebytecneMezery() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByEmail("novy@example.com")).willReturn(false);

        service.zmenEmail("admin@dokgen.local", "heslo", "  novy@example.com  ");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getEmail()).isEqualTo("novy@example.com");
    }

    @Test
    void zmenEmailSNespravnymHeslemVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("spatneheslo", "$2a$stareHash")).willReturn(false);

        assertThatThrownBy(() -> service.zmenEmail("admin@dokgen.local", "spatneheslo", "novy@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nesouhlasí");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenEmailSNeplatnymFormatemVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);

        assertThatThrownBy(() -> service.zmenEmail("admin@dokgen.local", "heslo", "neplatny-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formát");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenEmailSObsazenymEmailemVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);
        given(uzivatelRepository.existsByEmail("asistentka@dokgen.local")).willReturn(true);

        assertThatThrownBy(() -> service.zmenEmail("admin@dokgen.local", "heslo", "asistentka@dokgen.local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existuje");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenEmailNaStejnyEmailNepovazujeSeZaObsazeny() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("heslo", "$2a$stareHash")).willReturn(true);

        service.zmenEmail("admin@dokgen.local", "heslo", "admin@dokgen.local");

        verify(uzivatelRepository, never()).existsByEmail(any());
        verify(uzivatelRepository).save(any());
    }
}
