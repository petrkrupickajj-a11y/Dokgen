package cz.petrk.dokgen.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RegistraceServiceTest {

    private UzivatelRepository uzivatelRepository;
    private PasswordEncoder passwordEncoder;
    private RegistraceService service;

    @BeforeEach
    void setUp() {
        // Ciste unit testy bez Spring MVC pozadavku nemaji zadny LocaleResolver,
        // ktery by nastavil jazyk - bez tohohle by test byl zavisly na jazyce
        // stroje, na kterem bezi (LocaleContextHolder by spadl na Locale.getDefault()).
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        zpravy.setFallbackToSystemLocale(false);

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        service = new RegistraceService(uzivatelRepository, passwordEncoder, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void zaregistrujUlozNovehoUzivateleSZahashovanymHeslem() {
        given(uzivatelRepository.existsByEmail("novak@example.com")).willReturn(false);
        given(passwordEncoder.encode("tajneheslo123")).willReturn("$2a$hash");

        service.zaregistruj("novak@example.com", "tajneheslo123", "tajneheslo123");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getEmail()).isEqualTo("novak@example.com");
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("$2a$hash");
    }

    @Test
    void zaregistrujOriznePrebytecneMezeryVEmailu() {
        given(uzivatelRepository.existsByEmail("novak@example.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash");

        service.zaregistruj("  novak@example.com  ", "tajneheslo123", "tajneheslo123");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getEmail()).isEqualTo("novak@example.com");
    }

    @Test
    void zaregistrujPrevedeEmailNaMalaPismena() {
        given(uzivatelRepository.existsByEmail("novak@example.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash");

        service.zaregistruj("Novak@Example.COM", "tajneheslo123", "tajneheslo123");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getEmail()).isEqualTo("novak@example.com");
        verify(uzivatelRepository).existsByEmail("novak@example.com");
    }

    @Test
    void zaregistrujSNeshodujicimiSeHesyVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("novak@example.com", "heslo1234", "jineheslo9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neshodují");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSPrilisKratkymHeslemVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("novak@example.com", "abc", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSPrilisDlouhymHeslemVyhodiChybu() {
        String prilisDlouhe = "a".repeat(73);

        assertThatThrownBy(() -> service.zaregistruj("novak@example.com", prilisDlouhe, prilisDlouhe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("72");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSObsazenymEmailemVyhodiChybu() {
        given(uzivatelRepository.existsByEmail("admin@dokgen.local")).willReturn(true);

        assertThatThrownBy(() -> service.zaregistruj("admin@dokgen.local", "tajneheslo123", "tajneheslo123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existuje");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSPrazdnymEmailemVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("", "tajneheslo123", "tajneheslo123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("povinný");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSNeplatnymFormatemEmailuVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("novak-bez-zaviname", "tajneheslo123", "tajneheslo123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formát");

        verify(uzivatelRepository, never()).save(any());
    }
}
