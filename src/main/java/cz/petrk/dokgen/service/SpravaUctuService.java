package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.ResetHeslaRepository;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Schvalovani novych uctu z verejne registrace (viz Uzivatel.aktivni,
 * RegistraceService) a sprava existujicich uctu - stranka /uzivatele
 * (UzivateleController) odsud bere seznamy i akce Schvalit/Zamitnout/Smazat.
 */
@Service
public class SpravaUctuService {

    private final UzivatelRepository uzivatelRepository;
    private final ResetHeslaRepository resetHeslaRepository;
    private final MessageSource zpravy;

    public SpravaUctuService(UzivatelRepository uzivatelRepository, ResetHeslaRepository resetHeslaRepository,
                              MessageSource zpravy) {
        this.uzivatelRepository = uzivatelRepository;
        this.resetHeslaRepository = resetHeslaRepository;
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

    /**
     * Smaze existujici (aktivni) ucet - nejdriv jeho zaznamy ResetHesla (jinak
     * by mazani narazilo na cizi klic), pak samotny ucet. Nejde smazat ucet,
     * pod kterym je prave prihlaseny - posledni pojistka proti vymknuti.
     *
     * @Transactional je tu nutne explicitne - deleteByUzivatel je odvozeny
     * "delete" dotaz (ne findById/save/delete z CrudRepository, ktere uz
     * transakci maji z SimpleJpaRepository), takze bez vlastni transakce
     * kolem obou volani by spadl na TransactionRequiredException.
     */
    @Transactional
    public void smaz(Long id, String prihlasenyEmail) {
        Uzivatel uzivatel = uzivatelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.uzivatele.neexistuje")));
        if (EmailValidace.normalizuj(uzivatel.getEmail()).equals(EmailValidace.normalizuj(prihlasenyEmail))) {
            throw new IllegalArgumentException(zprava("chyba.uzivatele.nelze_smazat_sebe"));
        }

        resetHeslaRepository.deleteByUzivatel(uzivatel);
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
