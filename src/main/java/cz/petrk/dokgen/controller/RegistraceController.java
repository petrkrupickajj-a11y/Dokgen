package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.RegistraceService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistraceController {

    private final RegistraceService registraceService;

    public RegistraceController(RegistraceService registraceService) {
        this.registraceService = registraceService;
    }

    @GetMapping("/registrace")
    public String formular() {
        return "registrace";
    }

    @PostMapping("/registrace")
    public String zaregistrovat(@RequestParam String email,
                                 @RequestParam String heslo,
                                 @RequestParam String hesloZnovu,
                                 RedirectAttributes redirectAttributes) {
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
