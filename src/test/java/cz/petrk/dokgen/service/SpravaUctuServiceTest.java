package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.ResetHeslaRepository;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SpravaUctuServiceTest {

    private UzivatelRepository uzivatelRepository;
    private ResetHeslaRepository resetHeslaRepository;
    private SpravaUctuService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        resetHeslaRepository = Mockito.mock(ResetHeslaRepository.class);
        service = new SpravaUctuService(uzivatelRepository, resetHeslaRepository, zpravy);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getUctyVraciVsechnyUctySeRazenim() {
        List<Uzivatel> ucty = List.of(new Uzivatel("admin@dokgen.local", "hash"), new Uzivatel("novak@example.com", "hash"));
        given(uzivatelRepository.findAllByOrderByVytvorenoDneAsc()).willReturn(ucty);

        assertThat(service.getUcty()).isEqualTo(ucty);
    }

    @Test
    void smazOdstraniResetHeslaZaznamyIUcetSamotny() {
        Uzivatel cizi = new Uzivatel("novak@example.com", "hash");
        given(uzivatelRepository.findById(1L)).willReturn(Optional.of(cizi));

        service.smaz(1L, "admin@dokgen.local");

        verify(resetHeslaRepository).deleteByUzivatel(cizi);
        verify(uzivatelRepository).delete(cizi);
    }

    @Test
    void smazNeexistujicihoUctuVyhodiChybu() {
        given(uzivatelRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.smaz(1L, "admin@dokgen.local"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(uzivatelRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void smazVlastnihoUctuVyhodiChybuANesmazeHo() {
        Uzivatel prihlaseny = new Uzivatel("admin@dokgen.local", "hash");
        given(uzivatelRepository.findById(1L)).willReturn(Optional.of(prihlaseny));

        assertThatThrownBy(() -> service.smaz(1L, "admin@dokgen.local"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(uzivatelRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(resetHeslaRepository, never()).deleteByUzivatel(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void smazVlastnihoUctuJeNecitlivyNaVelikostPismenEmailu() {
        Uzivatel prihlaseny = new Uzivatel("admin@dokgen.local", "hash");
        given(uzivatelRepository.findById(1L)).willReturn(Optional.of(prihlaseny));

        assertThatThrownBy(() -> service.smaz(1L, "Admin@Dokgen.Local"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
