package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NastaveniController {

    private final MojeEmailService mojeEmailService;

    public NastaveniController(MojeEmailService mojeEmailService) {
        this.mojeEmailService = mojeEmailService;
    }

    @GetMapping("/nastaveni")
    public String formular(Authentication authentication, Model model) {
        model.addAttribute("aktualniEmail", authentication.getName());
        return "nastaveni";
    }

    @PostMapping("/nastaveni/email")
    public String zmenitEmail(@RequestParam String soucasneHeslo,
                               @RequestParam String novyEmail,
                               Authentication authentication,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) {
        try {
            mojeEmailService.zmenEmail(authentication.getName(), soucasneHeslo, novyEmail);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            return "redirect:/nastaveni";
        }

        // Zmena emailu meni identitu prihlaseneho uctu - stavajici session by
        // dal ukazovala stary email (SecurityContext se pri zmene v DB sam
        // neobnovi), takze uzivatele odhlasime a necháme ho prihlasit se
        // znovu pod novym emailem.
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?emailZmenen";
    }
}
