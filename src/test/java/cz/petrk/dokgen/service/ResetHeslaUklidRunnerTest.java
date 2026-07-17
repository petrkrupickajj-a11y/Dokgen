package cz.petrk.dokgen.service;

import cz.petrk.dokgen.repository.ResetHeslaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

class ResetHeslaUklidRunnerTest {

    private ResetHeslaRepository repository;
    private ResetHeslaUklidRunner runner;
    private Clock hodiny;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ResetHeslaRepository.class);
        hodiny = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC"));
        runner = new ResetHeslaUklidRunner(repository, hodiny);
    }

    @Test
    void priStartuSmazePouziteAProsleTokeny() {
        given(repository.deleteByPouzitTrueOrVyprsiDneBefore(any(LocalDateTime.class))).willReturn(3L);

        runner.run(new DefaultApplicationArguments());

        verify(repository).deleteByPouzitTrueOrVyprsiDneBefore(LocalDateTime.now(hodiny));
    }

    @Test
    void naplanovanyUklidDelaTotezJakoPriStartu() {
        given(repository.deleteByPouzitTrueOrVyprsiDneBefore(any(LocalDateTime.class))).willReturn(1L);

        runner.uklidNaplanovane();

        verify(repository).deleteByPouzitTrueOrVyprsiDneBefore(LocalDateTime.now(hodiny));
    }

    // Selhani uklidu (napr. docasne nedostupna DB) nesmi shodit start cele appky -
    // run() bezi jako ApplicationRunner primo v SpringApplication.run().
    @Test
    void selhaniUkliduPriStartuNevyhodiVyjimku() {
        willThrow(new RuntimeException("DB nedostupna"))
                .given(repository).deleteByPouzitTrueOrVyprsiDneBefore(any(LocalDateTime.class));

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
