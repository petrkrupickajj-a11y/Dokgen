package cz.petrk.dokgen.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vypocita castky pro polozky faktury - cenu a soucet kazde polozky (mnozstvi
 * krat cena za jednotku) a celkovou castku pres vsechny polozky. Vysledek uz
 * je v obecne Map<String,String> podobe, kterou umi zpracovat DocumentGeneratorService
 * (viz jeho konvence prefixu "${polozka.").
 *
 * BigDecimal vsude, nikdy double - binarni desetinna cisla neumi presne
 * reprezentovat destinne zlomky, coz by u penez vedlo k chybam v halerich.
 */
@Service
public class PolozkyVypocetService {

    private final MessageSource zpravy;

    public PolozkyVypocetService(MessageSource zpravy) {
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    /** Jedna polozka na vstupu - jeste bez vypocteneho poradi a soucinu. */
    public record PolozkaVstup(String nazev, BigDecimal mnozstvi, BigDecimal cena) {
    }

    /** Polozky uz s dopocitanym poradim a castkami (pro tabulku) a celkova castka pres vsechny (pro ${celkem}). */
    public record Vysledek(List<Map<String, String>> polozky, String celkem) {
    }

    public Vysledek spocti(List<PolozkaVstup> vstup) {
        // NumberFormat neni thread-safe - nova instance pro kazde volani. Sdilena
        // staticka instance by se pri soubeznych pozadavcich mohla poskodit.
        NumberFormat formatCastky = vytvorFormatCastky();

        List<Map<String, String>> polozky = new ArrayList<>();
        BigDecimal celkovySoucet = BigDecimal.ZERO;
        int poradi = 1;
        for (PolozkaVstup polozka : vstup) {
            overValidni(polozka);

            BigDecimal celkemRadku = polozka.mnozstvi().multiply(polozka.cena()).setScale(2, RoundingMode.HALF_UP);
            celkovySoucet = celkovySoucet.add(celkemRadku);

            Map<String, String> radek = new LinkedHashMap<>();
            radek.put("poradi", String.valueOf(poradi++));
            radek.put("nazev", polozka.nazev());
            radek.put("mnozstvi", polozka.mnozstvi().stripTrailingZeros().toPlainString());
            radek.put("cena", formatCastky.format(polozka.cena()));
            radek.put("celkem", formatCastky.format(celkemRadku));
            polozky.add(radek);
        }

        return new Vysledek(polozky, formatCastky.format(celkovySoucet));
    }

    private void overValidni(PolozkaVstup polozka) {
        if (polozka.nazev() == null || polozka.nazev().isBlank()) {
            throw new IllegalArgumentException(zprava("chyba.polozka.nazev_povinny"));
        }
        if (polozka.mnozstvi() == null || polozka.mnozstvi().signum() < 0) {
            throw new IllegalArgumentException(zprava("chyba.polozka.mnozstvi_zaporne"));
        }
        if (polozka.cena() == null || polozka.cena().signum() < 0) {
            throw new IllegalArgumentException(zprava("chyba.polozka.cena_zaporna"));
        }
    }

    // Vychozi DecimalFormatSymbols pro cs-CZ v aktualnim JDK pouziva jako oddelovac
    // tisicu tvrdou mezeru (  - CLDR data), ne bezny mezerovy znak - vysledek
    // by tak vypadal jako "1 234,50", ale kopie do textoveho pole/testu by
    // porovnavala jiny znak, nez vidi clovek. Nahrazujeme ho bezni mezerou, aby
    // format presne odpovidal pozadovanemu "1 234,50" ze zadani.
    private NumberFormat vytvorFormatCastky() {
        DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.forLanguageTag("cs-CZ"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        DecimalFormatSymbols symboly = format.getDecimalFormatSymbols();
        symboly.setGroupingSeparator(' ');
        format.setDecimalFormatSymbols(symboly);
        return format;
    }
}
