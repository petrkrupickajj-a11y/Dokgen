package cz.petrk.dokgen.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * Zachytává chyby, ktere by jinak skoncily na osklive Spring "Whitelabel
 * Error Page", a misto toho zobrazi srozumitelnou chybovou stranku.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource zpravy;

    public GlobalExceptionHandler(MessageSource zpravy) {
        this.zpravy = zpravy;
    }

    private String zprava(String kod) {
        return zpravy.getMessage(kod, null, LocaleContextHolder.getLocale());
    }

    // Neexistujici klient/sablona - vyhazuje se napr. z KlientController pri spatnem id v URL
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String neplatnyPozadavek(IllegalArgumentException ex, Model model) {
        model.addAttribute("zprava", ex.getMessage());
        return "chyba";
    }

    // Preklep v URL / neexistujici stranka - musi mit vlastni handler, jinak by ji
    // odchytil obecny Exception handler nize a chybne vratil 500 misto 404.
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String strankaNenalezena(Model model) {
        model.addAttribute("zprava", zprava("chyba.stranka_nenalezena"));
        return "chyba";
    }

    // Chyba pri cteni/generovani sablony (chybejici nebo poskozeny .docx soubor) -
    // DocumentGeneratorService uz sem posila srozumitelnou zpravu pro uzivatele
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String chybaGenerovaniDokumentu(IOException ex, Model model) {
        LOG.error("Chyba při generování dokumentu", ex);
        model.addAttribute("zprava", ex.getMessage());
        return "chyba";
    }

    // Nahravany soubor sablony presahl povoleny limit velikosti (viz application.properties)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String prilisVelkySoubor(Model model) {
        model.addAttribute("zprava", zprava("chyba.soubor_prilis_velky"));
        return "chyba";
    }

    // Vsechno ostatni neocekavane - uzivateli se detail chyby neukazuje, jen se zaloguje
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String neocekavanaChyba(Exception ex, Model model) {
        LOG.error("Neočekávaná chyba při zpracování požadavku", ex);
        model.addAttribute("zprava", zprava("chyba.neocekavana"));
        return "chyba";
    }
}
