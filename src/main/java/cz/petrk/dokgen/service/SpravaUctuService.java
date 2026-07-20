package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Uzivatel;
import cz.petrk.dokgen.repository.ResetHeslaRepository;
import cz.petrk.dokgen.repository.UzivatelRepository;
import cz.petrk.dokgen.util.EmailValidace;
import cz.petrk.dokgen.util.Vyhledani;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sprava existujicich uctu - stranka /uzivatele (UzivateleController) odsud
 * bere seznam uctu i akci Smazat.
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

    public List<Uzivatel> getUcty() {
        return uzivatelRepository.findAllByOrderByVytvorenoDneAsc();
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
        Uzivatel uzivatel = Vyhledani.najdiNeboVyhod(uzivatelRepository.findById(id), zprava("chyba.uzivatele.neexistuje"));
        if (EmailValidace.normalizuj(uzivatel.getEmail()).equals(EmailValidace.normalizuj(prihlasenyEmail))) {
            throw new IllegalArgumentException(zprava("chyba.uzivatele.nelze_smazat_sebe"));
        }

        resetHeslaRepository.deleteByUzivatel(uzivatel);
        uzivatelRepository.delete(uzivatel);
    }
}
