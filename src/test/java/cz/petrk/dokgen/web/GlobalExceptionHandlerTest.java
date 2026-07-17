package cz.petrk.dokgen.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler zajistuje, ze uzivatel nikdy nevidi Spring
 * Whitelabel Error Page - vsechny chyby konci na srozumitelne strance
 * chyba.html. "Neocekavana" (generic Exception) zamerne neukazuje detail
 * vyjimky uzivateli, jen obecnou hlasku (viz zprava.equals check nize).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler;

    GlobalExceptionHandlerTest() {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        handler = new GlobalExceptionHandler(zpravy);
    }

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(new Locale("cs"));
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void neplatnyPozadavekVratiChybovouStrankuSPuvodniZpravou() {
        Model model = new ExtendedModelMap();

        String view = handler.neplatnyPozadavek(new IllegalArgumentException("Klient s id 1 neexistuje"), model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Klient s id 1 neexistuje");
    }

    @Test
    void neplatnyVstupVratiChybovouStrankuSPuvodniZpravou() {
        Model model = new ExtendedModelMap();

        String view = handler.neplatnyVstup(new NeplatnyVstupException("Neplatný formát dokumentu \"EXE\"."), model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Neplatný formát dokumentu \"EXE\".");
    }

    @Test
    void strankaNenalezenaVratiChybovouStrankuSPratelskouZpravou() {
        Model model = new ExtendedModelMap();

        String view = handler.strankaNenalezena(model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Stránka nenalezena.");
    }

    @Test
    void chybaGenerovaniDokumentuVratiChybovouStrankuSPuvodniZpravou() {
        Model model = new ExtendedModelMap();

        String view = handler.chybaGenerovaniDokumentu(new IOException("Šablona je poškozená."), model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Šablona je poškozená.");
    }

    @Test
    void prilisVelkySouborVratiChybovouStrankuSPratelskouZpravou() {
        Model model = new ExtendedModelMap();

        String view = handler.prilisVelkySoubor(model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Nahrávaný soubor je příliš velký. Maximální povolená velikost je 5 MB.");
    }

    @Test
    void neocekavanaChybaVratiObecnouZpravuNeprozrazujiciDetailVyjimky() {
        Model model = new ExtendedModelMap();

        String view = handler.neocekavanaChyba(new RuntimeException("interní detail, který se nemá zobrazit"), model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isEqualTo("Nastala neočekávaná chyba. Zkus to prosím znovu.");
    }
}
