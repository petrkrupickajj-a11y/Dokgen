package cz.petrk.dokgen.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VyhledaniTest {

    @Test
    void vratiHodnotuKdyzOptionalNeniPrazdny() {
        assertThat(Vyhledani.najdiNeboVyhod(Optional.of("klient"), "nepouzije se")).isEqualTo("klient");
    }

    @Test
    void vyhodiIllegalArgumentSPredanouZpravouKdyzOptionalJePrazdny() {
        assertThatThrownBy(() -> Vyhledani.najdiNeboVyhod(Optional.empty(), "Klient s id 1 neexistuje"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Klient s id 1 neexistuje");
    }
}
