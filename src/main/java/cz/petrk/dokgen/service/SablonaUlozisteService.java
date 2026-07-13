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
 * Stara se o fyzicke ulozeni souboru sablon na disku (mimo classpath,
 * aby slo za behu appky pridavat nove sablony pres upload).
 * Databaze (entita Sablona) drzi jen metadata - zobrazovany nazev
 * a nazev souboru, ktery se najde v tomto adresari.
 */
@Service
public class SablonaUlozisteService {

    private final Path adresar;
    private final MessageSource zpravy;

    public SablonaUlozisteService(@Value("${dokgen.sablony.adresar:./data/word-templates}") String adresarCesta,
                                   MessageSource zpravy) throws IOException {
        this.adresar = Path.of(adresarCesta);
        this.zpravy = zpravy;
        Files.createDirectories(adresar);
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    private Path cesta(String nazevSouboru) {
        return adresar.resolve(nazevSouboru);
    }

    public boolean existuje(String nazevSouboru) {
        return Files.exists(cesta(nazevSouboru));
    }

    public byte[] nacti(String nazevSouboru) throws IOException {
        Path soubor = cesta(nazevSouboru);
        if (!Files.exists(soubor)) {
            throw new IOException(zprava("chyba.sablona.soubor_neexistuje", nazevSouboru));
        }
        return Files.readAllBytes(soubor);
    }

    public void uloz(String nazevSouboru, byte[] obsah) throws IOException {
        Files.write(cesta(nazevSouboru), obsah);
    }

    public void smaz(String nazevSouboru) throws IOException {
        Files.deleteIfExists(cesta(nazevSouboru));
    }

    /**
     * Otevre soubor sablony ve vychozi aplikaci OS (Word, LibreOffice...), stejne
     * jako by na nej uzivatel dvakrat klikl v Pruzkumniku. Funguje jen pri lokalnim
     * behu appky na stroji s grafickym rozhranim - na headless serveru (typicky
     * vzdalene nasazeni) java.awt.Desktop zadnou vychozi aplikaci nema jak spustit,
     * proto se to preventivne overi pred pokusem o otevreni.
     */
    public void otevriVeVychoziAplikaci(String nazevSouboru) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException(zprava("chyba.sablona.otevreni_nepodporovano"));
        }

        Path soubor = cesta(nazevSouboru);
        if (!Files.exists(soubor)) {
            throw new IOException(zprava("chyba.sablona.soubor_neexistuje", nazevSouboru));
        }
        Desktop.getDesktop().open(soubor.toFile());
    }
}
