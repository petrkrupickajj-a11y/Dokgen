package cz.petrk.dokgen.controller;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.KlientRepository;
import cz.petrk.dokgen.service.DocumentGeneratorService;
import cz.petrk.dokgen.service.HistorieService;
import cz.petrk.dokgen.service.PdfExportService;
import cz.petrk.dokgen.service.VygenerovanyDokumentUlozisteService;
import cz.petrk.dokgen.service.VysledekGenerovani;
import cz.petrk.dokgen.util.KlientData;
import cz.petrk.dokgen.util.NazevSouboru;
import cz.petrk.dokgen.util.Vyhledani;
import cz.petrk.dokgen.web.NeplatnyVstupException;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
public class KlientController {

    // Stejna velikost stranky jako HistorieService.VELIKOST_STRANKY - zadny
    // specialni duvod pro jinou hodnotu, jen konzistence napric appkou.
    private static final int VELIKOST_STRANKY = 20;

    private static final DateTimeFormatter FORMAT_DATA = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final KlientRepository klientRepository;
    private final DocumentGeneratorService documentGeneratorService;
    private final PdfExportService pdfExportService;
    private final HistorieService historieService;
    private final VygenerovanyDokumentUlozisteService vygenerovanyDokumentUloziste;
    private final MessageSource zpravy;

    public KlientController(KlientRepository klientRepository,
                             DocumentGeneratorService documentGeneratorService,
                             PdfExportService pdfExportService,
                             HistorieService historieService,
                             VygenerovanyDokumentUlozisteService vygenerovanyDokumentUloziste,
                             MessageSource zpravy) {
        this.klientRepository = klientRepository;
        this.documentGeneratorService = documentGeneratorService;
        this.pdfExportService = pdfExportService;
        this.historieService = historieService;
        this.vygenerovanyDokumentUloziste = vygenerovanyDokumentUloziste;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    // Datum vygenerovani patri do kontextu kazdeho dokumentu (${datum} pouzivaji
    // vsechny vestavene sablony), bez ohledu na to, jestli sablona ma i polozky.
    private Map<String, String> sestavKontext(Klient klient) {
        Map<String, String> kontext = new LinkedHashMap<>(KlientData.sestavKontext(klient));
        kontext.put("datum", LocalDate.now().format(FORMAT_DATA));
        return kontext;
    }

    // Uvodni stranka - seznam klientu, volitelne zuzeny hledanim (jmeno,
    // prijmeni, telefon nebo email) - uzitecne, jakmile klientu prubeha vic.
    // Strankovano stejne jako /historie - filtrovani i strankovani probiha
    // v pameti, pocet klientu v teto appce nikdy nebude tak velky, aby to vadilo.
    @GetMapping("/")
    public String seznam(@RequestParam(required = false) String hledat,
                          @RequestParam(defaultValue = "0") int strana,
                          Model model) {
        List<Klient> klienti = klientRepository.findAll(Sort.by("prijmeni", "jmeno"));
        if (hledat != null && !hledat.isBlank()) {
            String hledany = hledat.trim().toLowerCase(Locale.ROOT);
            klienti = klienti.stream()
                    .filter(k -> obsahuje(k.getJmeno(), hledany) || obsahuje(k.getPrijmeni(), hledany)
                            || obsahuje(k.getTelefon(), hledany) || obsahuje(k.getEmail(), hledany))
                    .toList();
        }

        int celkemStranek = Math.max(1, (int) Math.ceil(klienti.size() / (double) VELIKOST_STRANKY));
        int stranaOverena = Math.max(0, Math.min(strana, celkemStranek - 1));
        int od = stranaOverena * VELIKOST_STRANKY;
        List<Klient> stranka = klienti.subList(od, Math.min(od + VELIKOST_STRANKY, klienti.size()));

        model.addAttribute("klienti", stranka);
        model.addAttribute("hledat", hledat);
        model.addAttribute("strana", stranaOverena);
        model.addAttribute("celkemStranek", celkemStranek);
        return "seznam";
    }

    private boolean obsahuje(String pole, String hledany) {
        return pole != null && pole.toLowerCase(Locale.ROOT).contains(hledany);
    }

    // Formular pro noveho klienta
    @GetMapping("/novy")
    public String novyFormular(Model model) {
        model.addAttribute("klient", new Klient());
        return "formular";
    }

    // Formular pro upravu existujiciho klienta
    @GetMapping("/upravit/{id}")
    public String upravitFormular(@PathVariable Long id, Model model) {
        Klient klient = Vyhledani.najdiNeboVyhod(klientRepository.findById(id), zprava("chyba.klient.neexistuje", id));
        model.addAttribute("klient", klient);
        return "formular";
    }

    // Ulozeni noveho/upraveneho klienta (jeden endpoint na obojí - JPA pozna
    // podle vyplneneho/nevyplneneho id, jestli ma vlozit novy zaznam nebo upravit stavajici)
    @PostMapping("/ulozit")
    public String ulozit(@Valid @ModelAttribute Klient klient, BindingResult vysledekValidace) {
        if (vysledekValidace.hasErrors()) {
            return "formular";
        }
        klientRepository.save(klient);
        return "redirect:/";
    }

    @PostMapping("/smazat/{id}")
    public String smazat(@PathVariable Long id) {
        if (!klientRepository.existsById(id)) {
            throw new IllegalArgumentException(zprava("chyba.klient.neexistuje_smazat", id));
        }
        klientRepository.deleteById(id);
        return "redirect:/";
    }

    // Stranka s vyberem sablony pro konkretniho klienta
    @GetMapping("/generovat/{id}")
    public String vyberSablony(@PathVariable Long id, Model model) {
        Klient klient = Vyhledani.najdiNeboVyhod(klientRepository.findById(id), zprava("chyba.klient.neexistuje", id));
        model.addAttribute("klient", klient);
        model.addAttribute("sablony", documentGeneratorService.getDostupneSablony());
        return "generovat";
    }

    // Samotne vygenerovani a stazeni dokumentu (Word nebo PDF). Neplatny klient/sablona
    // (typicky smazani mezi nactenim /generovat/{id} a odeslanim formulare) vrati
    // uzivatele zpet na vyber sablony s vysvetlujici hlaskou - stejny princip jako
    // u SablonaController.nahrat/nahradit/smazat - misto obecne stranky chyba.html.
    @PostMapping("/generovat/{id}")
    public Object generujDokument(@PathVariable Long id,
                                   @RequestParam Long sablonaId,
                                   @RequestParam(defaultValue = "WORD") String format,
                                   RedirectAttributes redirectAttributes) throws IOException {
        String formatOvereny = overFormat(format);

        Klient klient;
        VysledekGenerovani vysledek;
        try {
            klient = Vyhledani.najdiNeboVyhod(klientRepository.findById(id), zprava("chyba.klient.neexistuje", id));
            vysledek = documentGeneratorService.vygenerujDokument(sablonaId, sestavKontext(klient), List.of());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("chybaGenerovani", e.getMessage());
            return "redirect:/generovat/" + id;
        }
        Sablona sablona = vysledek.sablona();

        byte[] dokument;
        String pripona;
        MediaType typ;
        if ("PDF".equals(formatOvereny)) {
            dokument = pdfExportService.prevedNaPdf(vysledek.obsah());
            pripona = ".pdf";
            typ = MediaType.APPLICATION_PDF;
        } else {
            dokument = vysledek.obsah();
            pripona = ".docx";
            typ = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }

        VygenerovanyDokument zaznam = historieService.zaznamenej(klient, sablona, formatOvereny);
        // Samotny soubor se uklada vedle audit zaznamu (viz VygenerovanyDokumentUlozisteService),
        // aby ho slo pozdeji znovu otevrit z /historie ("Zobrazit").
        vygenerovanyDokumentUloziste.uloz(zaznam.getId(), formatOvereny, dokument);

        String nazevSouboru = NazevSouboru.ocisti(sablona.getNazev()) + "_"
                + NazevSouboru.ocisti(klient.getPrijmeni()) + pripona;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(typ);
        headers.setContentDispositionFormData("attachment", nazevSouboru);

        return new ResponseEntity<>(dokument, headers, HttpStatus.OK);
    }

    // Format prijde jako obycejny textovy parametr formulare - normalni pouziti appky
    // posila jen "WORD"/"PDF" (viz generovat.html), ale primy HTTP pozadavek by mohl
    // poslat cokoliv jineho. Bez teto kontroly by se libovolny retezec ulozil do
    // historie (VygenerovanyDokument.format) a pouzil pri uklidu/otevirani souboru.
    private String overFormat(String format) {
        String formatVelkymi = format == null ? "" : format.toUpperCase(Locale.ROOT);
        if (!formatVelkymi.equals("WORD") && !formatVelkymi.equals("PDF")) {
            throw new NeplatnyVstupException(zprava("chyba.generovat.format_neplatny", format));
        }
        return formatVelkymi;
    }

    // Nahled dokumentu primo v prohlizeci pred stazenim - vzdy jako PDF (i kdyz
    // uzivatel ma ve formulari vybrany Word), protoze .docx se v prohlizeci
    // neda rozumne zobrazit. Na rozdil od /generovat/{id} (POST) se nahled
    // NEuklada do historie ani na disk - je to jen docasny pohled na obsah.
    @GetMapping("/generovat/{id}/nahled")
    public ResponseEntity<byte[]> nahledDokumentu(@PathVariable Long id,
                                                   @RequestParam Long sablonaId) throws IOException {
        Klient klient = Vyhledani.najdiNeboVyhod(klientRepository.findById(id), zprava("chyba.klient.neexistuje", id));

        VysledekGenerovani vysledek = documentGeneratorService.vygenerujDokument(sablonaId, sestavKontext(klient), List.of());
        byte[] pdf = pdfExportService.prevedNaPdf(vysledek.obsah());

        String nazevSouboru = NazevSouboru.ocisti(vysledek.sablona().getNazev()) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(nazevSouboru, StandardCharsets.UTF_8).build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
