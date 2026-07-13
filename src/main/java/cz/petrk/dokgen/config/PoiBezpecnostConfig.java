package cz.petrk.dokgen.config;

import jakarta.annotation.PostConstruct;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Configuration;

/**
 * Ochrana proti "zip bombe" - poskozenemu/zlomyslnemu .docx souboru (docx je
 * ve skutecnosti ZIP archiv), ktery se po rozbaleni nafoukne na obrovskou
 * velikost a spadne appku pameti/diskem. Apache POI uz ma zabudovanou
 * detekci pomeru komprese, tady ji jen zpresnujeme a pridavame tvrdy strop
 * na velikost jednoho souboru uvnitr ZIPu - plati pro kazde cteni .docx
 * v cele appce (upload sablony i generovani dokumentu).
 */
@Configuration
public class PoiBezpecnostConfig {

    @PostConstruct
    public void nastavLimityProZip() {
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(100L * 1024 * 1024);
    }
}
