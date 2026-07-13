package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.ResetHesla;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.ResetHeslaRepository;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ResetHeslaServiceTest {

    private UzivatelRepository uzivatelRepository;
    private ResetHeslaRepository resetHeslaRepository;
    private PasswordEncoder passwordEncoder;
    private EmailOdesilatel emailOdesilatel;
    private TestovaciHodiny hodiny;
    private ResetHeslaService service;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");

        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        resetHeslaRepository = Mockito.mock(ResetHeslaRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        emailOdesilatel = Mockito.mock(EmailOdesilatel.class);
        hodiny = new TestovaciHodiny(Instant.parse("2026-01-01T10:00:00Z"));
        service = new ResetHeslaService(uzivatelRepository, resetHeslaRepository, passwordEncoder, emailOdesilatel, zpravy, hodiny);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Uzivatel uzivatel() {
        return new Uzivatel("novak@example.com", "$2a$hash");
    }

    @Test
    void pozadejResetUProExistujiciEmailUlozTokenAPosleEmail() {
        given(uzivatelRepository.findByEmail("novak@example.com")).willReturn(Optional.of(uzivatel()));
        given(emailOdesilatel.odesli(anyString(), anyString(), anyString())).willReturn(true);

        service.pozadejReset("novak@example.com", "http://localhost:8080");

        verify(resetHeslaRepository).save(any(ResetHesla.class));
        verify(emailOdesilatel).odesli(eq("novak@example.com"), anyString(), anyString());
    }

    @Test
    void pozadejResetNajdeUcetIKdyzJeEmailZadanSJinouVelikostiPismen() {
        given(uzivatelRepository.findByEmail("novak@example.com")).willReturn(Optional.of(uzivatel()));
        given(emailOdesilatel.odesli(anyString(), anyString(), anyString())).willReturn(true);

        service.pozadejReset("  Novak@Example.COM  ", "http://localhost:8080");

        verify(uzivatelRepository).findByEmail("novak@example.com");
    }

    @Test
    void pozadejResetKdyzEmailSeNepodariOdeslatNevyhodiChybu() {
        given(uzivatelRepository.findByEmail("novak@example.com")).willReturn(Optional.of(uzivatel()));
        given(emailOdesilatel.odesli(anyString(), anyString(), anyString())).willReturn(false);

        service.pozadejReset("novak@example.com", "http://localhost:8080");

        verify(resetHeslaRepository).save(any(ResetHesla.class));
    }

    @Test
    void pozadejResetProNeexistujiciEmailNicNedelaAleNevyhodiChybu() {
        given(uzivatelRepository.findByEmail("neznamy@example.com")).willReturn(Optional.empty());

        service.pozadejReset("neznamy@example.com", "http://localhost:8080");

        verify(resetHeslaRepository, never()).save(any());
        verify(emailOdesilatel, never()).odesli(anyString(), anyString(), anyString());
    }

    @Test
    void pozadejResetSPrazdnymEmailemNicNedela() {
        service.pozadejReset("", "http://localhost:8080");
        service.pozadejReset(null, "http://localhost:8080");

        verify(resetHeslaRepository, never()).save(any());
        verify(uzivatelRepository, never()).findByEmail(any());
    }

    @Test
    void odkazVEmailuObsahujeZakladUrlABaseTokenu() {
        given(uzivatelRepository.findByEmail("novak@example.com")).willReturn(Optional.of(uzivatel()));

        service.pozadejReset("novak@example.com", "http://localhost:8080");

        ArgumentCaptor<String> telo = ArgumentCaptor.forClass(String.class);
        verify(emailOdesilatel).odesli(anyString(), anyString(), telo.capture());
        assertThat(telo.getValue()).contains("http://localhost:8080/nove-heslo?token=");
    }

    @Test
    void platnyTokenJeUznanJakoPlatny() {
        ResetHesla reset = new ResetHesla(uzivatel(), hashProNejakyToken(), LocalDateTime.now(hodiny).plusMinutes(30));
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));

        assertThat(service.jeTokenPlatny("cokoliv")).isTrue();
    }

    @Test
    void neexistujiciTokenNeniPlatny() {
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThat(service.jeTokenPlatny("cokoliv")).isFalse();
    }

    @Test
    void vyprselyTokenNeniPlatny() {
        ResetHesla reset = new ResetHesla(uzivatel(), hashProNejakyToken(), LocalDateTime.now(hodiny).minusMinutes(1));
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));

        assertThat(service.jeTokenPlatny("cokoliv")).isFalse();
    }

    @Test
    void jizPouzityTokenNeniPlatny() {
        ResetHesla reset = new ResetHesla(uzivatel(), hashProNejakyToken(), LocalDateTime.now(hodiny).plusMinutes(30));
        reset.setPouzit(true);
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));

        assertThat(service.jeTokenPlatny("cokoliv")).isFalse();
    }

    @Test
    void nastavNoveHesloProPlatnyTokenZmeniHesloAOznaciTokenJakoPouzity() {
        Uzivatel uzivatel = uzivatel();
        ResetHesla reset = new ResetHesla(uzivatel, hashProNejakyToken(), LocalDateTime.now(hodiny).plusMinutes(30));
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));
        given(passwordEncoder.encode("noveheslo123")).willReturn("$2a$novyHash");

        service.nastavNoveHeslo("cokoliv", "noveheslo123", "noveheslo123");

        assertThat(uzivatel.getHeslo()).isEqualTo("$2a$novyHash");
        assertThat(reset.isPouzit()).isTrue();
        verify(uzivatelRepository).save(uzivatel);
        verify(resetHeslaRepository, times(1)).save(reset);
    }

    @Test
    void nastavNoveHesloSNeplatnymTokenemVyhodiChybu() {
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.nastavNoveHeslo("neplatny", "noveheslo123", "noveheslo123"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void nastavNoveHesloSPrilisKratkymHeslemVyhodiChybu() {
        ResetHesla reset = new ResetHesla(uzivatel(), hashProNejakyToken(), LocalDateTime.now(hodiny).plusMinutes(30));
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));

        assertThatThrownBy(() -> service.nastavNoveHeslo("cokoliv", "abc", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň");

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void nastavNoveHesloSNeshodujicimiSeHeslyVyhodiChybu() {
        ResetHesla reset = new ResetHesla(uzivatel(), hashProNejakyToken(), LocalDateTime.now(hodiny).plusMinutes(30));
        given(resetHeslaRepository.findByTokenHash(any())).willReturn(Optional.of(reset));

        assertThatThrownBy(() -> service.nastavNoveHeslo("cokoliv", "noveheslo123", "jineheslo456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neshodují");

        verify(uzivatelRepository, never()).save(any());
    }

    private String hashProNejakyToken() {
        return "libovolny-hash";
    }

    /** Testovaci Clock s pevnym Instant - reset tokeny se v testu tvori s expiraci relativni k nemu. */
    private static final class TestovaciHodiny extends Clock {
        private final Instant cas;

        private TestovaciHodiny(Instant cas) {
            this.cas = cas;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return cas;
        }
    }
}
