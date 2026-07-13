package cz.petrk.dokgen.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "maChybu" je obycejny model atribut odvozeny z pozadavkoveho parametru ?error
 * (viz PrihlaseniController) - Thymeleaf sablona login.html ho pouziva pro
 * aria-invalid/aria-describedby na polich jmeno/heslo. Primo pres implicitni
 * objekt "param" to pouzit nejde - Thymeleaf takove pouziti mimo th:if/th:unless
 * odmita ("Instantiation of new objects... forbidden").
 */
class PrihlaseniControllerTest {

    private final PrihlaseniController controller;

    PrihlaseniControllerTest() {
        ResourceBundleMessageSource zpravy = new ResourceBundleMessageSource();
        zpravy.setBasename("messages");
        zpravy.setDefaultEncoding("UTF-8");
        controller = new PrihlaseniController(zpravy);
    }

    @Test
    void bezChybyNastaviMaChybuNaFalse() {
        Model model = new ExtendedModelMap();

        String view = controller.prihlaseni(null, model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("maChybu")).isEqualTo(false);
    }

    @Test
    void sChybouNastaviMaChybuNaTrue() {
        Model model = new ExtendedModelMap();

        String view = controller.prihlaseni("", model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("maChybu")).isEqualTo(true);
    }

    @Test
    void pristupOdeprenVratiChybovouStrankuSeZpravou() {
        Model model = new ExtendedModelMap();

        String view = controller.pristupOdepren(model);

        assertThat(view).isEqualTo("chyba");
        assertThat(model.getAttribute("zprava")).isNotNull();
    }
}
