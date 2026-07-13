package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.ResetHeslaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class ResetHeslaController {

    private final ResetHeslaService resetHeslaService;

    public ResetHeslaController(ResetHeslaService resetHeslaService) {
        this.resetHeslaService = resetHeslaService;
    }

    @GetMapping("/zapomenute-heslo")
    public String formular() {
        return "zapomenute-heslo";
    }

    @PostMapping("/zapomenute-heslo")
    public String odeslat(@RequestParam String email, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String zakladUrl = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build().toUriString();
        resetHeslaService.pozadejReset(email, zakladUrl);
        redirectAttributes.addFlashAttribute("odeslano", true);
        return "redirect:/zapomenute-heslo";
    }

    @GetMapping("/nove-heslo")
    public String formularNoveHeslo(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenPlatny", resetHeslaService.jeTokenPlatny(token));
        return "nove-heslo";
    }

    @PostMapping("/nove-heslo")
    public String nastavitNoveHeslo(@RequestParam String token,
                                     @RequestParam String noveHeslo,
                                     @RequestParam String noveHesloZnovu,
                                     RedirectAttributes redirectAttributes) {
        try {
            resetHeslaService.nastavNoveHeslo(token, noveHeslo, noveHesloZnovu);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            return "redirect:/nove-heslo?token=" + token;
        }
        return "redirect:/login?hesloResetovano";
    }
}
