package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import cz.petrk.dokgen.service.HistorieService;
import cz.petrk.dokgen.service.VygenerovanyDokumentUlozisteService;
import cz.petrk.dokgen.util.NazevSouboru;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Vyhledavani v historii vygenerovanych dokumentu - filtrovatelne podle
 * mesice/roku a jmena klienta - a otevirani samotnych vygenerovanych souboru.
 */
@Controller
public class HistorieController {

    private final HistorieService historieService;
    private final VygenerovanyDokumentRepository vygenerovanyDokumentRepository;
    private final VygenerovanyDokumentUlozisteService uloziste;
    private final MessageSource zpravy;

    public HistorieController(HistorieService historieService,
                               VygenerovanyDokumentRepository vygenerovanyDokumentRepository,
                               VygenerovanyDokumentUlozisteService uloziste,
                               MessageSource zpravy) {
        this.historieService = historieService;
        this.vygenerovanyDokumentRepository = vygenerovanyDokumentRepository;
        this.uloziste = uloziste;
        this.zpravy = zpravy;
    }

    @GetMapping("/historie")
    public String historie(@RequestParam(required = false) Integer rok,
                            @RequestParam(required = false) Integer mesic,
                            @RequestParam(required = false) String jmeno,
                            @RequestParam(defaultValue = "0") int strana,
                            Model model) {
        model.addAttribute("zaznamy", historieService.vyhledej(rok, mesic, jmeno, strana));
        model.addAttribute("rok", rok);
        model.addAttribute("mesic", mesic);
        model.addAttribute("jmeno", jmeno);
        model.addAttribute("strana", strana);
        model.addAttribute("celkemStranek", historieService.celkemStranek(rok, mesic, jmeno));
        model.addAttribute("celkemZaznamu", historieService.celkemZaznamu(rok, mesic, jmeno));
        model.addAttribute("aktualniRok", LocalDate.now().getYear());
        model.addAttribute("mesice", nazvyMesicu());
        return "historie";
    }

    // Otevre skutecny vygenerovany soubor pro jeden zaznam historie - u PDF primo
    // v prohlizeci (inline), u Wordu ve vychozi aplikaci OS (stejny princip jako
    // "Upravit" u sablon, viz VygenerovanyDokumentUlozisteService). Vraci Object,
    // protoze podle formatu bud posle telo souboru (ResponseEntity), nebo presmeruje
    // zpet na /historie s infomackou (redirect: retezec) - Spring rozliší podle
    // skutecneho typu navratove hodnoty za behu.
    @GetMapping("/historie/{id}/zobrazit")
    public Object zobrazit(@PathVariable Long id, RedirectAttributes redirectAttributes) throws IOException {
        var mozneZaznamy = vygenerovanyDokumentRepository.findById(id);
        if (mozneZaznamy.isEmpty()) {
            redirectAttributes.addFlashAttribute("chybaHistorie",
                    zpravy.getMessage("chyba.historie.neexistuje", new Object[]{id}, LocaleContextHolder.getLocale()));
            return "redirect:/historie";
        }
        VygenerovanyDokument zaznam = mozneZaznamy.get();

        if ("PDF".equalsIgnoreCase(zaznam.getFormat())) {
            byte[] obsah = uloziste.nacti(zaznam.getId(), zaznam.getFormat());
            String nazevSouboru = NazevSouboru.ocisti(zaznam.getSablonaNazev()) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename(nazevSouboru, StandardCharsets.UTF_8).build());

            return new ResponseEntity<>(obsah, headers, HttpStatus.OK);
        }

        uloziste.otevriVeVychoziAplikaci(zaznam.getId(), zaznam.getFormat());
        redirectAttributes.addFlashAttribute("otevrenyDokument", zaznam.getSablonaNazev());
        return "redirect:/historie";
    }

    // Nazvy mesicu se berou z aktualniho jazyka appky (ne natvrdo cestiny), aby
    // filtr v /historie mluvil stejnym jazykem jako zbytek stranky.
    private List<String> nazvyMesicu() {
        List<String> nazvy = new ArrayList<>();
        for (Month mesic : Month.values()) {
            nazvy.add(mesic.getDisplayName(java.time.format.TextStyle.FULL, LocaleContextHolder.getLocale()));
        }
        return nazvy;
    }
}
