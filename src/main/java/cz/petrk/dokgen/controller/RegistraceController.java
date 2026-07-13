package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.IpOmezovac;
import cz.petrk.dokgen.service.RegistraceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistraceController {

    private final RegistraceService registraceService;
    private final IpOmezovac ipOmezovac;
    private final MessageSource zpravy;

    public RegistraceController(RegistraceService registraceService, IpOmezovac ipOmezovac, MessageSource zpravy) {
        this.registraceService = registraceService;
        this.ipOmezovac = ipOmezovac;
        this.zpravy = zpravy;
    }

    @GetMapping("/registrace")
    public String formular() {
        return "registrace";
    }

    @PostMapping("/registrace")
    public String zaregistrovat(@RequestParam String email,
                                 @RequestParam String heslo,
                                 @RequestParam String hesloZnovu,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ipOmezovac.povolPozadavek(request.getRemoteAddr())) {
            redirectAttributes.addFlashAttribute("chyba",
                    zpravy.getMessage("chyba.prilis_mnoho_pozadavku", null, LocaleContextHolder.getLocale()));
            redirectAttributes.addFlashAttribute("zadanyEmail", email);
            return "redirect:/registrace";
        }
        try {
            registraceService.zaregistruj(email, heslo, hesloZnovu);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            redirectAttributes.addFlashAttribute("zadanyEmail", email);
            return "redirect:/registrace";
        }
        return "redirect:/login?registrovano";
    }
}
