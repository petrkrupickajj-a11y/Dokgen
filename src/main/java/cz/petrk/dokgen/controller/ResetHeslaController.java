package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.ResetHeslaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResetHeslaController {

    private final ResetHeslaService resetHeslaService;
    private final String zakladUrl;

    public ResetHeslaController(ResetHeslaService resetHeslaService,
                                 @Value("${dokgen.zaklad-url}") String zakladUrl) {
        this.resetHeslaService = resetHeslaService;
        this.zakladUrl = zakladUrl;
    }

    @GetMapping("/zapomenute-heslo")
    public String formular() {
        return "zapomenute-heslo";
    }

    // Zaklad URL bereme z konfigurace (dokgen.zaklad-url), NIKDY z pozadavku -
    // Host hlavicku plne ovlada odesilatel pozadavku, takze by si utocnik mohl
    // pozadat o reset ciziho emailu s podvrzenou Host hlavickou a obeti by
    // prisel jinak platny odkaz vedouci na utocnikovu domenu (viz ResetHeslaService).
    @PostMapping("/zapomenute-heslo")
    public String odeslat(@RequestParam String email, RedirectAttributes redirectAttributes) {
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
