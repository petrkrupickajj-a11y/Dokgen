package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.SablonaUlozisteService;
import cz.petrk.dokgen.util.NazevSouboru;
import cz.petrk.dokgen.util.Vyhledani;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sprava sablon primo z webu - seznam, upload nove sablony (.docx), stazeni,
 * nahrazeni obsahu existujici sablony (napr. po uprave ve Wordu nebo Google
 * dokumentech) a smazani sablony (i vestavene - viz poznamka u
 * DocumentGeneratorService.smazSablonu()).
 */
@Controller
public class SablonaController {

    private final DocumentGeneratorService documentGeneratorService;
    private final SablonaRepository sablonaRepository;
    private final SablonaUlozisteService uloziste;
    private final MessageSource zpravy;

    public SablonaController(DocumentGeneratorService documentGeneratorService,
                              SablonaRepository sablonaRepository,
                              SablonaUlozisteService uloziste,
                              MessageSource zpravy) {
        this.documentGeneratorService = documentGeneratorService;
        this.sablonaRepository = sablonaRepository;
        this.uloziste = uloziste;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    @GetMapping("/sablony")
    public String seznam(Model model) {
        // Na /generovat se sablony radi abecedne (getDostupneSablony) - tady na
        // sprave je ale uzitecnejsi videt naposledy upravenou sablonu nahore.
        List<Sablona> sablony = new ArrayList<>(documentGeneratorService.getDostupneSablony());
        sablony.sort(Comparator.comparing(Sablona::getNahranoDne).reversed());
        model.addAttribute("sablony", sablony);
        return "sablony";
    }

    @PostMapping("/sablony/nahrat")
    public String nahrat(@RequestParam String nazev,
                          @RequestParam("soubor") MultipartFile soubor,
                          RedirectAttributes redirectAttributes) throws IOException {
        try {
            if (nazev == null || nazev.isBlank()) {
                throw new IllegalArgumentException(zprava("chyba.sablona.nazev_povinny"));
            }
            if (soubor.isEmpty()) {
                throw new IllegalArgumentException(zprava("chyba.sablona.soubor_povinny"));
            }
            documentGeneratorService.nahrajNovouSablonu(nazev.trim(), soubor);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chybaNahrani", e.getMessage());
        }
        return "redirect:/sablony";
    }

    // Stazeni aktualniho obsahu sablony - napr. pro upravu ve Wordu/Google dokumentech
    @GetMapping("/sablony/{id}/stahnout")
    public ResponseEntity<byte[]> stahnout(@PathVariable Long id) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));
        byte[] obsah = documentGeneratorService.stahniSablonu(id);

        String nazevSouboru = NazevSouboru.ocisti(sablona.getNazev()) + ".docx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", nazevSouboru);

        return new ResponseEntity<>(obsah, headers, HttpStatus.OK);
    }

    // Nahrazeni obsahu existujici sablony upravenou verzi (stejne id, stejny nazev)
    @PostMapping("/sablony/{id}/nahradit")
    public String nahradit(@PathVariable Long id,
                            @RequestParam("soubor") MultipartFile soubor,
                            RedirectAttributes redirectAttributes) throws IOException {
        try {
            documentGeneratorService.nahradSouborSablony(id, soubor);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chybaNahrani", e.getMessage());
        }
        return "redirect:/sablony";
    }

    // Otevre soubor sablony ve vychozi aplikaci OS (Word, LibreOffice...) - funguje jen
    // pri lokalnim behu appky na stroji s grafickym rozhranim, viz SablonaUlozisteService.
    // Nevalidni stav (headless server, chybejici soubor) zaminerne NEODCHYTAVAME lokalne -
    // proleti jako IOException do GlobalExceptionHandler, ktery ukaze srozumitelnou chyba.html.
    @PostMapping("/sablony/{id}/upravit")
    public String upravit(@PathVariable Long id, RedirectAttributes redirectAttributes) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));

        uloziste.otevriVeVychoziAplikaci(sablona.getNazevSouboru());

        redirectAttributes.addFlashAttribute("otevrenaSablona", sablona.getNazev());
        return "redirect:/sablony";
    }

    @PostMapping("/sablony/smazat/{id}")
    public String smazat(@PathVariable Long id, RedirectAttributes redirectAttributes) throws IOException {
        try {
            documentGeneratorService.smazSablonu(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chybaNahrani", e.getMessage());
        }
        return "redirect:/sablony";
    }

    // Historie starsich verzi sablony - vznikaji pri kazdem nahrazeni obsahu (viz nahradit())
    @GetMapping("/sablony/{id}/verze")
    public String verze(@PathVariable Long id, Model model) {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));
        model.addAttribute("sablona", sablona);
        model.addAttribute("verze", documentGeneratorService.getVerze(id));
        return "verze";
    }

    @GetMapping("/sablony/{id}/verze/{verzeId}/stahnout")
    public ResponseEntity<byte[]> stahnoutVerzi(@PathVariable Long id, @PathVariable Long verzeId) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));
        byte[] obsah = documentGeneratorService.stahniVerzi(id, verzeId);

        String nazevSouboru = NazevSouboru.ocisti(sablona.getNazev()) + "_verze.docx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", nazevSouboru);

        return new ResponseEntity<>(obsah, headers, HttpStatus.OK);
    }

    @PostMapping("/sablony/{id}/verze/{verzeId}/obnovit")
    public String obnovitVerzi(@PathVariable Long id, @PathVariable Long verzeId,
                                RedirectAttributes redirectAttributes) throws IOException {
        documentGeneratorService.obnovVerzi(id, verzeId);
        redirectAttributes.addFlashAttribute("verzeObnovena", true);
        return "redirect:/sablony/" + id + "/verze";
    }
}
