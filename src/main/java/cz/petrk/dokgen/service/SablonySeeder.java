package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.repository.SmazanaVestavenaSablonaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pri startu appky zajisti, ze vestavenych 5 sablon existuje jak v databazi
 * (metadata), tak na disku (skutecny .docx soubor) - i kdyz adresar
 * ./data/word-templates jeste neexistuje (prvni spusteni appky).
 *
 * Vyjimka: pokud uzivatel vestavenou sablonu pres /sablony umyslne smazal,
 * zustane po ni "tombstone" zaznam (SmazanaVestavenaSablona) a seeder ji
 * uz znovu nezaklada - smazani vestavene sablony je tak trvale.
 */
@Component
public class SablonySeeder implements ApplicationRunner {

    private static final Map<String, String> VESTAVENE_SABLONY = new LinkedHashMap<>();

    static {
        VESTAVENE_SABLONY.put("Smlouva o poskytování služeb", "smlouva.docx");
        VESTAVENE_SABLONY.put("Cenová nabídka", "nabidka.docx");
        VESTAVENE_SABLONY.put("Faktura", "faktura.docx");
        VESTAVENE_SABLONY.put("Protokol o předání", "protokol.docx");
        VESTAVENE_SABLONY.put("Plná moc", "plna_moc.docx");
    }

    private final SablonaRepository sablonaRepository;
    private final SablonaUlozisteService uloziste;
    private final SmazanaVestavenaSablonaRepository smazaneVestaveneRepository;

    public SablonySeeder(SablonaRepository sablonaRepository,
                          SablonaUlozisteService uloziste,
                          SmazanaVestavenaSablonaRepository smazaneVestaveneRepository) {
        this.sablonaRepository = sablonaRepository;
        this.uloziste = uloziste;
        this.smazaneVestaveneRepository = smazaneVestaveneRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Map.Entry<String, String> polozka : VESTAVENE_SABLONY.entrySet()) {
            String nazev = polozka.getKey();
            String nazevSouboru = polozka.getValue();

            if (smazaneVestaveneRepository.existsByNazev(nazev)) {
                continue;
            }

            if (!uloziste.existuje(nazevSouboru)) {
                try (InputStream vstup = new ClassPathResource("word-templates/" + nazevSouboru).getInputStream()) {
                    uloziste.uloz(nazevSouboru, vstup.readAllBytes());
                }
            }

            if (!sablonaRepository.existsByNazev(nazev)) {
                sablonaRepository.save(new Sablona(nazev, nazevSouboru, true));
            }
        }
    }
}
