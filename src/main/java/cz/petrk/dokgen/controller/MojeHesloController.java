package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeHesloService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MojeHesloController {

    private final MojeHesloService mojeHesloService;

    public MojeHesloController(MojeHesloService mojeHesloService) {
        this.mojeHesloService = mojeHesloService;
    }

    @GetMapping("/moje-heslo")
    public String formular() {
        return "moje-heslo";
    }

    @PostMapping("/moje-heslo")
    public String zmenit(@RequestParam String soucasneHeslo,
                          @RequestParam String noveHeslo,
                          @RequestParam String noveHesloZnovu,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            mojeHesloService.zmenHeslo(authentication.getName(), soucasneHeslo, noveHeslo, noveHesloZnovu);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            return "redirect:/moje-heslo";
        }
        redirectAttributes.addFlashAttribute("uspech", true);
        return "redirect:/moje-heslo";
    }
}
