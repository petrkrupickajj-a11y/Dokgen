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
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);
        given(passwordEncoder.encode("tajneheslo")).willReturn("$2a$hash");

        service.zaregistruj("novak", "tajneheslo", "tajneheslo", "ASISTENTKA");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getJmeno()).isEqualTo("novak");
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("$2a$hash");
    }

    @Test
    void zaregistrujOriznePrebytecneMezeryVeJmene() {
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash");

        service.zaregistruj("  novak  ", "tajneheslo", "tajneheslo", "ASISTENTKA");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getJmeno()).isEqualTo("novak");
    }

    @Test
    void zaregistrujSNeshodujicimiSeHesyVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("novak", "heslo1234", "jineheslo", "ASISTENTKA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neshodují");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSPrilisKratkymHeslemVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("novak", "abc", "abc", "ASISTENTKA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSObsazenymJmenemVyhodiChybu() {
        given(uzivatelRepository.existsByJmeno("admin")).willReturn(true);

        assertThatThrownBy(() -> service.zaregistruj("admin", "tajneheslo", "tajneheslo", "ASISTENTKA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obsazené");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujSPrazdnymJmenemVyhodiChybu() {
        assertThatThrownBy(() -> service.zaregistruj("", "tajneheslo", "tajneheslo", "ASISTENTKA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("povinné");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void zaregistrujUlozZadanouRoli() {
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash");

        service.zaregistruj("novak", "tajneheslo", "tajneheslo", "ADMIN");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getRole()).isEqualTo(cz.petrk.dokgen.entity.Role.ADMIN);
    }

    @Test
    void zaregistrujRoleJeNecitliveNaVelikostPismenABileZnaky() {
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("hash");

        service.zaregistruj("novak", "tajneheslo", "tajneheslo", " asistentka ");

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getRole()).isEqualTo(cz.petrk.dokgen.entity.Role.ASISTENTKA);
    }

    @Test
    void zaregistrujSNeplatnouRoliVyhodiChybu() {
        given(uzivatelRepository.existsByJmeno("novak")).willReturn(false);

        assertThatThrownBy(() -> service.zaregistruj("novak", "tajneheslo", "tajneheslo", "SUPERVISOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roli");

        verify(uzivatelRepository, never()).save(any());
    }
}
