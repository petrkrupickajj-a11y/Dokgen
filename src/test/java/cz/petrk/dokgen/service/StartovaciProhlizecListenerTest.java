package cz.petrk.dokgen.service;

import org.junit.jupiter.api.Test;

class StartovaciProhlizecListenerTest {

    @Test
    void vypnutaVolbaProhlizecVubecNezkousiOtevrit() {
        StartovaciProhlizecListener listener = new StartovaciProhlizecListener(false, "8080");

        // Volba je vypnuta, takze se metoda vrati hned na zacatku a vubec
        // se nedotkne java.awt.Desktop - bezpecne i na headless CI.
        listener.otevriProhlizec();
    }
}
