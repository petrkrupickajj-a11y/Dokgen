package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.service.MojeJmenoService;
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

    private final MojeJmenoService mojeJmenoService;

    public NastaveniController(MojeJmenoService mojeJmenoService) {
        this.mojeJmenoService = mojeJmenoService;
    }

    @GetMapping("/nastaveni")
    public String formular(Authentication authentication, Model model) {
        model.addAttribute("aktualniJmeno", authentication.getName());
        return "nastaveni";
    }

    @PostMapping("/nastaveni/jmeno")
    public String zmenitJmeno(@RequestParam String soucasneHeslo,
                               @RequestParam String noveJmeno,
                               Authentication authentication,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) {
        try {
            mojeJmenoService.zmenJmeno(authentication.getName(), soucasneHeslo, noveJmeno);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chyba", e.getMessage());
            return "redirect:/nastaveni";
        }

        // Zmena jmena meni identitu prihlaseneho uctu - stavajici session by
        // dal ukazovala stare jmeno (SecurityContext se pri zmene v DB sam
        // neobnovi), takze uzivatele odhlasime a necháme ho prihlasit se
        // znovu pod novym jmenem.
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?jmenoZmeneno";
    }
}
