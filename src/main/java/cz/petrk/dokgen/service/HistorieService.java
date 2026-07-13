package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Audit log vygenerovanych dokumentu - kdo, kdy, jakou sablonu a v jakem
 * formatu vygeneroval. Slouzi pro vyhledavani/filtrovani na strance /historie.
 */
@Service
public class HistorieService {

    public static final int VELIKOST_STRANKY = 20;

    private final VygenerovanyDokumentRepository repository;

    public HistorieService(VygenerovanyDokumentRepository repository) {
        this.repository = repository;
    }

    public VygenerovanyDokument zaznamenej(Klient klient, Sablona sablona, String format) {
        String jmenoPrijmeni = klient.getJmeno() + " " + klient.getPrijmeni();
        return repository.save(new VygenerovanyDokument(klient.getId(), jmenoPrijmeni, sablona.getNazev(), format));
    }

    /**
     * Vraci jednu stranku zaznamu (0-indexovanou) serazenych od nejnovejsiho,
     * volitelne omezenych na konkretni rok/mesic a/nebo hledani podle jmena
     * klienta. Filtrovani i strankovani probiha primo v pameti - pocet
     * zaznamu v teto appce nikdy nebude tak velky, aby to vadilo, a vyhneme
     * se tim slozitostem s datovymi funkcemi v JPQL/H2.
     */
    public List<VygenerovanyDokument> vyhledej(Integer rok, Integer mesic, String jmenoKlienta, int strana) {
        List<VygenerovanyDokument> filtrovane = filtrovaneZaznamy(rok, mesic, jmenoKlienta);

        int od = Math.max(strana, 0) * VELIKOST_STRANKY;
        if (od >= filtrovane.size()) {
            return List.of();
        }
        return filtrovane.subList(od, Math.min(od + VELIKOST_STRANKY, filtrovane.size()));
    }

    /** Kolik stran celkem existuje pro dany filtr (aspon 1, i kdyz je vysledek prazdny). */
    public int celkemStranek(Integer rok, Integer mesic, String jmenoKlienta) {
        return Math.max(1, (int) Math.ceil(celkemZaznamu(rok, mesic, jmenoKlienta) / (double) VELIKOST_STRANKY));
    }

    /** Kolik zaznamu celkem odpovida danemu filtru (napric vsemi strankami). */
    public int celkemZaznamu(Integer rok, Integer mesic, String jmenoKlienta) {
        return filtrovaneZaznamy(rok, mesic, jmenoKlienta).size();
    }

    private List<VygenerovanyDokument> filtrovaneZaznamy(Integer rok, Integer mesic, String jmenoKlienta) {
        String hledaniJmena = jmenoKlienta == null ? null : jmenoKlienta.trim().toLowerCase(Locale.ROOT);

        return repository.findAllByOrderByVytvorenoDneDesc().stream()
                .filter(d -> rok == null || d.getVytvorenoDne().getYear() == rok)
                .filter(d -> mesic == null || d.getVytvorenoDne().getMonthValue() == mesic)
                .filter(d -> hledaniJmena == null || hledaniJmena.isEmpty()
                        || d.getKlientJmenoPrijmeni().toLowerCase(Locale.ROOT).contains(hledaniJmena))
                .toList();
    }
}
