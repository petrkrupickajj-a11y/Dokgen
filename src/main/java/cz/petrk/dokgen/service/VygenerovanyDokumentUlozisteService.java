package cz.petrk.dokgen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fyzicke ulozeni vygenerovanych dokumentu na disk (VygenerovanyDokument v
 * databazi je jen audit zaznam - kdo/kdy/jakou sablonu, viz HistorieService).
 * Soubor se pojmenuje podle id zaznamu, aby ho slo jednoznacne dohledat pri
 * otevirani z /historie ("Zobrazit"). Stejny princip jako SablonaUlozisteService
 * pro sablony.
 */
@Service
public class VygenerovanyDokumentUlozisteService {

    private final Path adresar;
    private final MessageSource zpravy;

    public VygenerovanyDokumentUlozisteService(
            @Value("${dokgen.vygenerovane-dokumenty.adresar:./data/generated-documents}") String adresarCesta,
            MessageSource zpravy) throws IOException {
        this.adresar = Path.of(adresarCesta);
        this.zpravy = zpravy;
        Files.createDirectories(adresar);
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    private Path cesta(Long id, String format) {
        String pripona = "PDF".equalsIgnoreCase(format) ? ".pdf" : ".docx";
        return adresar.resolve(id + pripona);
    }

    public void uloz(Long id, String format, byte[] obsah) throws IOException {
        Files.write(cesta(id, format), obsah);
    }

    public boolean existuje(Long id, String format) {
        return Files.exists(cesta(id, format));
    }

    public byte[] nacti(Long id, String format) throws IOException {
        Path soubor = cesta(id, format);
        if (!Files.exists(soubor)) {
            throw new IOException(zprava("chyba.dokument.neexistuje", id));
        }
        return Files.readAllBytes(soubor);
    }

    /**
     * Smaze fyzicky soubor vygenerovaneho dokumentu z disku (napr. pri
     * uklidu starych dokumentu, viz GenerovaneDokumentyUklidRunner). Audit
     * zaznam v databazi (VygenerovanyDokument) tim NEmizi - /historie u nej
     * pak jen misto tlacitka "Zobrazit" ukaze "Soubor není dostupný", stejne
     * jako u starsich zaznamu z doby pred zavedenim ukladani souboru.
     */
    public boolean smaz(Long id, String format) throws IOException {
        return Files.deleteIfExists(cesta(id, format));
    }

    /**
     * Otevre Word dokument ve vychozi aplikaci OS, stejny princip jako
     * SablonaUlozisteService.otevriVeVychoziAplikaci() u sablon - funguje jen
     * pri lokalnim behu appky na stroji s grafickym rozhranim.
     */
    public void otevriVeVychoziAplikaci(Long id, String format) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException(zprava("chyba.dokument.otevreni_nepodporovano"));
        }

        Path soubor = cesta(id, format);
        if (!Files.exists(soubor)) {
            throw new IOException(zprava("chyba.dokument.neexistuje", id));
        }
        Desktop.getDesktop().open(soubor.toFile());
    }
}
