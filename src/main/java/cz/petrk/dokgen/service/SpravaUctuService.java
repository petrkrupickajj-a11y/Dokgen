package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.UzivatelRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Schvalovani novych uctu z verejne registrace (viz Uzivatel.aktivni,
 * RegistraceService) - stranka /uzivatele (UzivateleController) odsud bere
 * seznamy i akce Schvalit/Zamitnout.
 */
@Service
public class SpravaUctuService {

    private final UzivatelRepository uzivatelRepository;
    private final MessageSource zpravy;

    public SpravaUctuService(UzivatelRepository uzivatelRepository, MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    public List<Uzivatel> getCekajiciUcty() {
        return uzivatelRepository.findByAktivniFalseOrderByVytvorenoDneAsc();
    }

    public List<Uzivatel> getAktivniUcty() {
        return uzivatelRepository.findByAktivniTrueOrderByVytvorenoDneAsc();
    }

    /** Schvali cekajici ucet, aby se od ted mohl prihlasit. */
    public void schval(Long id) {
        Uzivatel uzivatel = najdiCekajici(id);
        uzivatel.setAktivni(true);
        uzivatelRepository.save(uzivatel);
    }

    /** Zamitnuti cekajici zadosti o ucet ucet rovnou smaze - nikdy nebyl aktivni, neni co zachovavat. */
    public void zamitni(Long id) {
        Uzivatel uzivatel = najdiCekajici(id);
        uzivatelRepository.delete(uzivatel);
    }

    private Uzivatel najdiCekajici(Long id) {
        Uzivatel uzivatel = uzivatelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.uzivatele.neexistuje")));
        if (uzivatel.jeAktivni()) {
            throw new IllegalArgumentException(zprava("chyba.uzivatele.jiz_aktivni"));
        }
        return uzivatel;
    }
}
