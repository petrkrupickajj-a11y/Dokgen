package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ZmenaHeslaRunnerTest {

    private UzivatelRepository uzivatelRepository;
    private PasswordEncoder passwordEncoder;
    private ZmenaHeslaRunner runner;

    @BeforeEach
    void setUp() {
        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
        runner = new ZmenaHeslaRunner(uzivatelRepository, passwordEncoder, context);
    }

    @Test
    void bezVolbyNicNedelaAVraciNull() {
        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments());

        assertThat(kod).isNull();
        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void existujicimuUzivateliZmeniHeslo() {
        Uzivatel existujici = new Uzivatel("admin", "staryHash");
        given(uzivatelRepository.findByJmeno("admin")).willReturn(Optional.of(existujici));
        given(passwordEncoder.encode("noveheslo")).willReturn("novyHash");

        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments("--zmenit-heslo=admin:noveheslo"));

        assertThat(kod).isEqualTo(0);
        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("novyHash");
    }

    @Test
    void neexistujicimuUzivateliVytvoriNovyUcet() {
        given(uzivatelRepository.findByJmeno("novak")).willReturn(Optional.empty());
        given(passwordEncoder.encode("noveheslo")).willReturn("novyHash");

        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments("--zmenit-heslo=novak:noveheslo"));

        assertThat(kod).isEqualTo(0);
        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getJmeno()).isEqualTo("novak");
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("novyHash");
    }

    @Test
    void chybejiciOddelovacVraciChybovyKodANicNeuklada() {
        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments("--zmenit-heslo=novakbezhesla"));

        assertThat(kod).isEqualTo(1);
        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void prazdneJmenoVraciChybovyKodANicNeuklada() {
        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments("--zmenit-heslo=:noveheslo"));

        assertThat(kod).isEqualTo(1);
        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void prazdneHesloVraciChybovyKodANicNeuklada() {
        Integer kod = runner.zpracujVolbu(new DefaultApplicationArguments("--zmenit-heslo=novak:"));

        assertThat(kod).isEqualTo(1);
        verify(uzivatelRepository, never()).save(any());
    }
}
