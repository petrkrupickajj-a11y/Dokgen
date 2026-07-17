package cz.petrk.dokgen.util;

import java.util.Optional;

/**
 * Sjednocuje opakovany vzor "najdi zaznam podle id, nebo vyhod IllegalArgumentException
 * se srozumitelnou zpravou", ktery se jinak opakoval skoro identicky v kazdem
 * controlleru/service, kde se pracuje s repository.findById(...).
 */
public final class Vyhledani {

    private Vyhledani() {
    }

    public static <T> T najdiNeboVyhod(Optional<T> vysledek, String zprava) {
        return vysledek.orElseThrow(() -> new IllegalArgumentException(zprava));
    }
}
