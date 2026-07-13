package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Konzolovy nastroj pro zmenu/obnoveni hesla, kdyby ho nekdo zapomnel.
 * Spusti se stejne jako appka, jen s navic argumentem - appka pak jen
 * zmeni (nebo, pokud ucet jeste neexistuje, rovnou vytvori) heslo v databazi
 * a hned skonci, bez bootovani weboveho serveru:
 *
 *   mvnw spring-boot:run -Dspring-boot.run.arguments="--zmenit-heslo=email:nove-heslo"
 *
 * Pozor: heslo nesmi obsahovat carku (Spring by ji vylozil jako oddelovac
 * vice hodnot argumentu).
 */
@Component
public class ZmenaHeslaRunner implements ApplicationRunner {

    private static final String VOLBA = "zmenit-heslo";

    private final UzivatelRepository uzivatelRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfigurableApplicationContext context;

    public ZmenaHeslaRunner(UzivatelRepository uzivatelRepository,
                             PasswordEncoder passwordEncoder,
                             ConfigurableApplicationContext context) {
        this.uzivatelRepository = uzivatelRepository;
        this.passwordEncoder = passwordEncoder;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer kodUkonceni = zpracujVolbu(args);
        if (kodUkonceni != null) {
            ukonci(kodUkonceni);
        }
    }

    /**
     * Vraci exit kod appky, pokud byla volba --zmenit-heslo zadana, jinak null
     * (appka pokracuje normalnim startem). Oddeleno od run(), aby se dalo
     * testovat bez skutecneho System.exit() v ukonci().
     */
    Integer zpracujVolbu(ApplicationArguments args) {
        if (!args.containsOption(VOLBA)) {
            return null;
        }

        List<String> hodnoty = args.getOptionValues(VOLBA);
        String hodnota = (hodnoty == null || hodnoty.isEmpty()) ? "" : hodnoty.get(0);
        int oddelovac = hodnota.indexOf(':');

        if (oddelovac <= 0 || oddelovac == hodnota.length() - 1) {
            System.out.println("Použití: --zmenit-heslo=email:nove-heslo");
            return 1;
        }

        String email = hodnota.substring(0, oddelovac);
        String noveHeslo = hodnota.substring(oddelovac + 1);
        String hash = passwordEncoder.encode(noveHeslo);

        Uzivatel uzivatel = uzivatelRepository.findByEmail(email).orElse(null);
        if (uzivatel == null) {
            uzivatelRepository.save(new Uzivatel(email, hash));
            System.out.println("Uživatel \"" + email + "\" neexistoval, byl nově vytvořen s tímto heslem.");
        } else {
            uzivatel.setHeslo(hash);
            uzivatelRepository.save(uzivatel);
            System.out.println("Heslo pro uživatele \"" + email + "\" bylo změněno.");
        }

        return 0;
    }

    private void ukonci(int kod) {
        System.exit(SpringApplication.exit(context, () -> kod));
    }
}
