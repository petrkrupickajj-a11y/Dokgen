package cz.petrk.dokgen.service;

import cz.petrk.dokgen.config.UzivateleProperties;
import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UzivateleSeederTest {

    private UzivatelRepository uzivatelRepository;
    private UzivateleProperties properties;
    private UzivateleSeeder seeder;

    @BeforeEach
    void setUp() {
        uzivatelRepository = Mockito.mock(UzivatelRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        given(passwordEncoder.encode(any())).willAnswer(vyvolani -> "hash:" + vyvolani.getArgument(0));
        properties = new UzivateleProperties();
        seeder = new UzivateleSeeder(properties, uzivatelRepository, passwordEncoder);
    }

    private void pridejUcet(String email, String heslo) {
        UzivateleProperties.Ucet ucet = new UzivateleProperties.Ucet();
        ucet.setEmail(email);
        ucet.setHeslo(heslo);
        properties.getUzivatele().add(ucet);
    }

    @Test
    void jizExistujiciUcetSeNeprepisuje() {
        pridejUcet("admin@dokgen.local", "zadaneHeslo123");
        given(uzivatelRepository.existsByEmail("admin@dokgen.local")).willReturn(true);

        seeder.run(new DefaultApplicationArguments());

        verify(uzivatelRepository, never()).save(any());
    }

    @Test
    void novyUcetSNastavenymHeslemPouzijeToto() {
        pridejUcet("admin@dokgen.local", "zadaneHeslo123");
        given(uzivatelRepository.existsByEmail("admin@dokgen.local")).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getHeslo()).isEqualTo("hash:zadaneHeslo123");
    }

    @Test
    void novyUcetBezNastavenehoHeslaDostaneNahodneVygenerovaneHeslo() {
        pridejUcet("admin@dokgen.local", "");
        given(uzivatelRepository.existsByEmail("admin@dokgen.local")).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getHeslo()).startsWith("hash:").isNotEqualTo("hash:");
    }

    @Test
    void dvaUctyBezHeslaDostanouKazdyJineNahodneHeslo() {
        pridejUcet("admin@dokgen.local", null);
        pridejUcet("asistentka@dokgen.local", null);
        given(uzivatelRepository.existsByEmail(any())).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository, Mockito.times(2)).save(zachyceny.capture());
        assertThat(zachyceny.getAllValues().get(0).getHeslo())
                .isNotEqualTo(zachyceny.getAllValues().get(1).getHeslo());
    }

    @Test
    void seedovanyUcetJeRovnouAktivni() {
        pridejUcet("admin@dokgen.local", "zadaneHeslo123");
        given(uzivatelRepository.existsByEmail("admin@dokgen.local")).willReturn(false);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<Uzivatel> zachyceny = ArgumentCaptor.forClass(Uzivatel.class);
        verify(uzivatelRepository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().jeAktivni()).isTrue();
    }

    @Test
    void existujiciUcetBezPriznakuAktivniVDatabaziDostaneDodatecneAktivni() {
        // Simuluje ucet vytvoreny pred zavedenim schvalovani - Hibernate ddl-auto=update
        // pro nej pridal sloupec "aktivni" jako null.
        Uzivatel legacyUcet = new Uzivatel("stary-ucet@dokgen.local", "hash");
        legacyUcet.setAktivni(null);
        given(uzivatelRepository.findAll()).willReturn(java.util.List.of(legacyUcet));

        seeder.run(new DefaultApplicationArguments());

        assertThat(legacyUcet.jeAktivni()).isTrue();
        verify(uzivatelRepository).save(legacyUcet);
    }

    @Test
    void existujiciUcetSJizNastavenymAktivniSeNeprepisuje() {
        Uzivatel neaktivniUcet = new Uzivatel("cekajici@example.com", "hash", false);
        given(uzivatelRepository.findAll()).willReturn(java.util.List.of(neaktivniUcet));

        seeder.run(new DefaultApplicationArguments());

        verify(uzivatelRepository, never()).save(neaktivniUcet);
        assertThat(neaktivniUcet.jeAktivni()).isFalse();
    }
}
