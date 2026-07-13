package cz.petrk.dokgen.controller;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class PrihlaseniController {

    private final MessageSource zpravy;

    public PrihlaseniController(MessageSource zpravy) {
        this.zpravy = zpravy;
    }

    // Chybu (?error) i duvod odhlaseni appka dostane primo jako pozadavkovy parametr od
    // Spring Security (formLogin/logout presmeruji sem s ?error resp. ?odhlaseno v URL).
    // Prevadime ji tu na obycejny model atribut "maChybu" - Thymeleaf ma pristup k
    // request parametrum pres specialni objekt "param", ktery ale nejde pouzit jinde
    // nez v th:if/th:unless (mimo tyto kontexty Thymeleaf jeho pouziti odmitne).
    @GetMapping("/login")
    public String prihlaseni(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("maChybu", error != null);
        return "login";
    }

    // Cil accessDeniedHandler (SecurityConfig), kdyby autorizace z nejakeho duvodu selhala -
    // @RequestMapping bez metody, protoze forward zachova puvodni HTTP metodu pozadavku (GET i POST).
    @RequestMapping("/pristup-odepren")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String pristupOdepren(Model model) {
        model.addAttribute("zprava", zpravy.getMessage("chyba.pristup_odepren", null, LocaleContextHolder.getLocale()));
        return "chyba";
    }
}
