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

class MojeHesloServiceTest {

    private UzivatelRepository uzivatelRepository;
    private PasswordEncoder passwordEncoder;
    private MojeHesloService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new MojeHesloService(uzivatelRepository, passwordEncoder, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Uzivatel uzivatel() {
        return new Uzivatel("admin@dokgen.local", "$2a$stareHash", Role.ADMIN);
    }

    @Test
    void zmenHesloUlozNovyHashKdySoucasneHesloSouhlasi() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("stareheslo", "$2a$stareHash")).willReturn(true);
        given(passwordEncoder.encode("noveheslo123")).willReturn("$2a$novyHash");

        service.zmenHeslo("admin@dokgen.local", "stareheslo", "noveheslo123", "noveheslo123");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("$2a$novyHash");
    }

    @Test
    void zmenHesloSNespravnymSoucasnymHeslemVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("spatneheslo", "$2a$stareHash")).willReturn(false);

        assertThatThrownBy(() -> service.zmenHeslo("admin@dokgen.local", "spatneheslo", "noveheslo123", "noveheslo123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nesouhlasí");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenHesloSPrilisKratkymNovymHeslemVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("stareheslo", "$2a$stareHash")).willReturn(true);

        assertThatThrownBy(() -> service.zmenHeslo("admin@dokgen.local", "stareheslo", "abc", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zmenHesloSNeshodujicimiSeNovymiHeslyVyhodiChybu() {
        given(uzivatelRepository.findByEmail("admin@dokgen.local")).willReturn(Optional.of(uzivatel()));
        given(passwordEncoder.matches("stareheslo", "$2a$stareHash")).willReturn(true);

        assertThatThrownBy(() -> service.zmenHeslo("admin@dokgen.local", "stareheslo", "noveheslo1", "noveheslo2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neshodují");

        verify(uzivatelRepository, never()).save(any());
    }
}
