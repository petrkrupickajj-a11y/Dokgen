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
    public String zaregistrovat(@RequestParam String jmeno,
                                 @RequestParam String heslo,
                                 @RequestParam String hesloZnovu,
                                 @RequestParam(defaultValue = "ASISTENTKA") String role,
                                 RedirectAttributes redirectAttributes) {
        try {
            registraceService.zaregistruj(jmeno, heslo, hesloZnovu, role);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            redirectAttributes.addFlashAttribute("zadaneJmeno", jmeno);
            redirectAttributes.addFlashAttribute("zadanaRole", role);
            return "redirect:/registrace";
        }
        redirectAttributes.addFlashAttribute("uspech", jmeno);
        return "redirect:/registrace";
    }
}
