package cz.petrk.dokgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailZeStarehoJmenaRunnerTest {

    private JdbcTemplate jdbcTemplate;
    private EmailZeStarehoJmenaRunner runner;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        runner = new EmailZeStarehoJmenaRunner(jdbcTemplate);
        sloupecJmenoExistuje(true);
    }

    private void sloupecJmenoExistuje(boolean existuje) {
        given(jdbcTemplate.queryForObject(contains("INFORMATION_SCHEMA.COLUMNS"), eq(Integer.class)))
                .willReturn(existuje ? 1 : 0);
    }

    private void ucetBezEmailu(long id, String jmeno) {
        given(jdbcTemplate.queryForList(contains("EMAIL IS NULL")))
                .willReturn(List.of(Map.of("ID", id, "JMENO", jmeno)));
    }

    private void emailUzPatriJinemuUctu(String email, boolean patri) {
        given(jdbcTemplate.queryForObject(contains("COUNT(*) FROM UZIVATEL WHERE EMAIL"), eq(Integer.class), eq(email)))
                .willReturn(patri ? 1 : 0);
    }

    @Test
    void staremuUctuSEmailemVeJmenuSeDoplniEmail() {
        ucetBezEmailu(67L, "petr.krupicka.jj@gmail.com");
        emailUzPatriJinemuUctu("petr.krupicka.jj@gmail.com", false);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).update(contains("UPDATE UZIVATEL"), eq("petr.krupicka.jj@gmail.com"), eq(67L));
    }

    @Test
    void emailZeJmenaSeUloziNormalizovany() {
        ucetBezEmailu(67L, "  Petr.Krupicka.JJ@Gmail.com ");
        emailUzPatriJinemuUctu("petr.krupicka.jj@gmail.com", false);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).update(contains("UPDATE UZIVATEL"), eq("petr.krupicka.jj@gmail.com"), eq(67L));
    }

    @Test
    void ucetSeJmenemKtereNeniEmailZustaneBezeZmeny() {
        ucetBezEmailu(98L, "recenzent");

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void ucetJehozEmailUzPatriJinemuZustaneBezeZmeny() {
        ucetBezEmailu(67L, "admin@dokgen.local");
        emailUzPatriJinemuUctu("admin@dokgen.local", true);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void bezSloupceJmenoSeNicNedeje() {
        sloupecJmenoExistuje(false);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).queryForList(anyString());
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }
}
