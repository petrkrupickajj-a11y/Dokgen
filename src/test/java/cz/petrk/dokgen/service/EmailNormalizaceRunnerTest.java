package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNormalizaceRunnerTest {

    private UzivatelRepository uzivatelRepository;
    private EmailNormalizaceRunner runner;

    @BeforeEach
    void setUp() {
        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        runner = new EmailNormalizaceRunner(uzivatelRepository);
    }

    @Test
    void ucetSJizNormalizovanymEmailemSeNemeni() {
        Uzivatel jizNormalizovany = new Uzivatel("novak@example.com", "hash");
        given(uzivatelRepository.findAll()).willReturn(List.of(jizNormalizovany));

        runner.run(new DefaultApplicationArguments());

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void ucetSVelkymiPismenyDostaneNormalizovanyEmail() {
        Uzivatel stary = new Uzivatel("Novak@Example.COM", "hash");
        given(uzivatelRepository.findAll()).willReturn(List.of(stary));

        runner.run(new DefaultApplicationArguments());

        assertThat(stary.getEmail()).isEqualTo("novak@example.com");
        verify(uzivatelRepository).save(stary);
    }

    @Test
    void kolidujiciUctyPoNormalizaciSeNemeniAJenSeZaloguje() {
        Uzivatel velkyma = new Uzivatel("Novak@Example.com", "hash1");
        Uzivatel malyma = new Uzivatel("novak@example.com", "hash2");
        given(uzivatelRepository.findAll()).willReturn(List.of(velkyma, malyma));

        runner.run(new DefaultApplicationArguments());

        assertThat(velkyma.getEmail()).isEqualTo("Novak@Example.com");
        assertThat(malyma.getEmail()).isEqualTo("novak@example.com");
        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void ucetBezEmailuSeNechaBezeZmenyProEmailZeStarehoJmenaRunner() {
        Uzivatel starySUzivatelskymJmenem = new Uzivatel(null, "hash");
        given(uzivatelRepository.findAll()).willReturn(List.of(starySUzivatelskymJmenem));

        runner.run(new DefaultApplicationArguments());

        assertThat(starySUzivatelskymJmenem.getEmail()).isNull();
        verify(uzivatelRepository, never()).save(any());
    }
}
