package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.SpravaUctuService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UzivateleController {

    private final SpravaUctuService spravaUctuService;

    public UzivateleController(SpravaUctuService spravaUctuService) {
        this.spravaUctuService = spravaUctuService;
    }

    @GetMapping("/uzivatele")
    public String seznam(Authentication authentication, Model model) {
        model.addAttribute("cekajici", spravaUctuService.getCekajiciUcty());
        model.addAttribute("aktivni", spravaUctuService.getAktivniUcty());
        model.addAttribute("aktualniEmail", authentication.getName());
        return "uzivatele";
    }

    @PostMapping("/uzivatele/{id}/schvalit")
    public String schvalit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            spravaUctuService.schval(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
        }
        return "redirect:/uzivatele";
    }

    @PostMapping("/uzivatele/{id}/zamitnout")
    public String zamitnout(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            spravaUctuService.zamitni(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
        }
        return "redirect:/uzivatele";
    }

    @PostMapping("/uzivatele/{id}/smazat")
    public String smazat(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            spravaUctuService.smaz(id, authentication.getName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
        }
        return "redirect:/uzivatele";
    }
}
