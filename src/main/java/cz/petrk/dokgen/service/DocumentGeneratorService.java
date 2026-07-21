package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.SablonaVerze;
import cz.petrk.dokgen.entity.SmazanaVestavenaSablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.repository.SablonaVerzeRepository;
import cz.petrk.dokgen.repository.SmazanaVestavenaSablonaRepository;
import cz.petrk.dokgen.util.Vyhledani;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Srdce cele aplikace.
 *
 * Princip je jednoduchy:
 *  1. Najdeme Sablona zaznam v databazi podle id a nacteme jeho .docx soubor
 *     z adresare spravovaneho SablonaUlozisteService.
 *  2. Projdeme vsechny odstavce v tele, zahlavi i zapati dokumentu, vcetne
 *     bunek tabulek (i vnorenych) - viz nahradVTele - a hledame placeholdery
 *     typu ${jmeno}, ${prijmeni} atd.
 *  3. Kde placeholder najdeme, nahradime ho skutecnou hodnotou z dodaneho kontextu.
 *  4. Vratime vysledny .docx jako pole bajtu -> to pak posleme uzivateli
 *     ke stazeni v controlleru.
 *
 * Generator zamerne nezna zadnou domenovou entitu (Klient, faktura...) - dostava
 * hotovou Map<String,String> s hodnotami placeholderu. Prevod domenovych dat
 * na tuhle obecnou podobu je vec volajiciho (napr. KlientController pouziva
 * KlientData.sestavKontext).
 *
 * PRIDANI NOVE SABLONY se dnes dela primo v appce na strance /sablony
 * (nahranim .docx souboru s placeholdery ${jmeno}, ${prijmeni}, ${telefon},
 * ${email}, ${adresa}, ${mesto}, ${psc}, ${ico}, ${poznamka}, ${datum}).
 */
@Service
public class DocumentGeneratorService {

    private final SablonaRepository sablonaRepository;
    private final SablonaUlozisteService uloziste;
    private final SmazanaVestavenaSablonaRepository smazaneVestaveneRepository;
    private final SablonaVerzeRepository sablonaVerzeRepository;
    private final MessageSource zpravy;

    public DocumentGeneratorService(SablonaRepository sablonaRepository,
                                     SablonaUlozisteService uloziste,
                                     SmazanaVestavenaSablonaRepository smazaneVestaveneRepository,
                                     SablonaVerzeRepository sablonaVerzeRepository,
                                     MessageSource zpravy) {
        this.sablonaRepository = sablonaRepository;
        this.uloziste = uloziste;
        this.smazaneVestaveneRepository = smazaneVestaveneRepository;
        this.sablonaVerzeRepository = sablonaVerzeRepository;
        this.zpravy = zpravy;
    }

    private String zprava(String kod, Object... args) {
        return zpravy.getMessage(kod, args, LocaleContextHolder.getLocale());
    }

    public List<Sablona> getDostupneSablony() {
        return sablonaRepository.findAll(Sort.by("nazev"));
    }

    public VysledekGenerovani vygenerujDokument(Long sablonaId, Map<String, String> kontext,
                                                 List<Map<String, String>> polozky) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(sablonaId), zprava("chyba.sablona.neznama", sablonaId));

        Pattern vzorPlaceholderu = sestavVzorPlaceholderu(kontext.keySet());

        try (ByteArrayInputStream vstup = new ByteArrayInputStream(uloziste.nacti(sablona.getNazevSouboru()));
             XWPFDocument dokument = new XWPFDocument(vstup);
             ByteArrayOutputStream vystup = new ByteArrayOutputStream()) {

            // Opakovani sablonoveho radku tabulky musi probehnout pred obecnym
            // nahrazenim placeholderu - nove vlozene radky pak projdou stejnym
            // pruchodem nize spolu se zbytkem dokumentu (napr. kdyby polozka
            // nahodou obsahovala i jiny nez ${polozka.*} placeholder).
            zpracujTabulkyPolozek(dokument, polozky);

            // Telo dokumentu (odstavce i tabulky vcetne vnorenych - viz nahradVTele)
            nahradVTele(dokument, kontext, vzorPlaceholderu);

            // Zahlavi a zapati - dokument jich muze mit vic (pro prvni/lichou/sudou stranku)
            for (XWPFHeader zahlavi : dokument.getHeaderList()) {
                nahradVTele(zahlavi, kontext, vzorPlaceholderu);
            }
            for (XWPFFooter zapati : dokument.getFooterList()) {
                nahradVTele(zapati, kontext, vzorPlaceholderu);
            }

            dokument.write(vystup);
            return new VysledekGenerovani(vystup.toByteArray(), sablona);
        } catch (IOException e) {
            if (jeZipBomba(e)) {
                throw new IOException(zprava("chyba.sablona.zip_bomba_generovani", sablona.getNazev()), e);
            }
            throw new IOException(zprava("chyba.sablona.nacteni_selhalo", sablona.getNazev()), e);
        } catch (RuntimeException e) {
            // POI pri poskozenem/nevalidnim .docx souboru casto hazi nekontrolovanou
            // vyjimku (napr. POIXMLException) misto IOException
            throw new IOException(zprava("chyba.sablona.poskozena", sablona.getNazev()), e);
        }
    }

    /**
     * Zjisti, jestli sablona pouziva konvenci opakovani radku (obsahuje niekde
     * sablonovy radek s ${polozka.) - pro /generovat/{id}, kde formular
     * zobrazuje sekci s polozkami jen u takovych sablon.
     *
     * Sablonu pri kazdem volani znovu nacte a projde, misto aby si priznak
     * ulozila primo do entity Sablona - jednodussi cesta bez rizika, ze by
     * ulozeny priznak zastaral po nahrazeni obsahu souboru pres "Nahradit" na
     * /sablony, a bez potreby DB migrace. Poctem sablon v teto appce (jednotky
     * az desitky) je dodatecne cteni souboru pri kazdem zobrazeni /generovat/{id}
     * zanedbatelne.
     */
    public boolean sablonaObsahujePolozky(Sablona sablona) throws IOException {
        try (ByteArrayInputStream vstup = new ByteArrayInputStream(uloziste.nacti(sablona.getNazevSouboru()));
             XWPFDocument dokument = new XWPFDocument(vstup)) {
            return obsahujeSablonovyRadekKdekoliv(dokument);
        } catch (IOException e) {
            throw new IOException(zprava("chyba.sablona.nacteni_selhalo", sablona.getNazev()), e);
        } catch (RuntimeException e) {
            throw new IOException(zprava("chyba.sablona.poskozena", sablona.getNazev()), e);
        }
    }

    private boolean obsahujeSablonovyRadekKdekoliv(IBody telo) {
        for (XWPFTable tabulka : telo.getTables()) {
            if (najdiIndexSablonovehoRadku(tabulka) >= 0) {
                return true;
            }
            for (XWPFTableRow radek : tabulka.getRows()) {
                for (XWPFTableCell bunka : radek.getTableCells()) {
                    if (obsahujeSablonovyRadekKdekoliv(bunka)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Nahraje novou sablonu pres upload z /sablony. Soubor se nejdriv
     * overi, ze jde vubec otevrit jako platny .docx, aby se poskozeny/spatny
     * soubor odmitl hned pri nahravani a ne az pri prvnim pokusu o generovani.
     */
    public Sablona nahrajNovouSablonu(String nazev, MultipartFile soubor) throws IOException {
        if (sablonaRepository.existsByNazev(nazev)) {
            throw new IllegalArgumentException(zprava("chyba.sablona.nazev_existuje", nazev));
        }

        byte[] obsah = soubor.getBytes();
        overPlatnyDocx(obsah);

        String nazevSouboru = UUID.randomUUID() + ".docx";
        uloziste.uloz(nazevSouboru, obsah);
        return sablonaRepository.save(new Sablona(nazev, nazevSouboru, false));
    }

    /**
     * Smaze sablonu vcetne jejiho souboru na disku - jde i o vestavenou
     * sablonu. Pokud jde o vestavenou sablonu, zapise se navic "tombstone"
     * zaznam (SmazanaVestavenaSablona), aby ji SablonySeeder pri pristim
     * startu appky znovu needelal - smazani vestavene sablony je tedy trvale.
     */
    public void smazSablonu(Long id) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));

        List<SablonaVerze> verze = sablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc(id);
        for (SablonaVerze v : verze) {
            uloziste.smaz(v.getNazevSouboru());
        }
        sablonaVerzeRepository.deleteAll(verze);

        uloziste.smaz(sablona.getNazevSouboru());
        sablonaRepository.delete(sablona);
        if (sablona.isVestavena()) {
            smazaneVestaveneRepository.save(new SmazanaVestavenaSablona(sablona.getNazev()));
        }
    }

    /**
     * Stahne aktualni obsah sablony - napr. pro upravu ve Wordu, Google
     * dokumentech nebo jinem nastroji mimo appku.
     */
    public byte[] stahniSablonu(Long id) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));
        return uloziste.nacti(sablona.getNazevSouboru());
    }

    /**
     * Nahradi obsah existujici sablony (stejne id, stejny nazev) upravenou
     * verzi souboru - napr. po editaci ve Wordu/Google dokumentech a
     * zpetnem exportu do .docx. Funguje i pro vestavene sablony.
     *
     * Synchronized - jde o cteni-uprava-zapis stejneho souboru na disku
     * (nejdriv se aktualni obsah ulozi jako verze, pak se prepise novym),
     * takze dva soubezne pozadavky (i na ruzne sablony) by se bez zamku
     * mohly prekryvat. Pro appku pouzivanou jednotkami lidi v ramci jedne
     * firmy je jednoduchy zamek na cele metode dostatecny - sprava sablon
     * neni operace, ktera by potrebovala vysokou propustnost.
     */
    public synchronized void nahradSouborSablony(Long id, MultipartFile novySoubor) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(id), zprava("chyba.sablona.neexistuje", id));

        if (novySoubor.isEmpty()) {
            throw new IllegalArgumentException(zprava("chyba.sablona.soubor_povinny"));
        }

        byte[] obsah = novySoubor.getBytes();
        overPlatnyDocx(obsah);

        ulozAktualniObsahJakoVerzi(sablona);

        uloziste.uloz(sablona.getNazevSouboru(), obsah);
        sablona.oznacUpraveno();
        sablonaRepository.save(sablona);
    }

    /**
     * Vypis starsich verzi sablony (od nejnovejsi) - vznikaji pri kazdem
     * nahrazeni obsahu (viz nahradSouborSablony) i pred obnovenim jine verze.
     */
    public List<SablonaVerze> getVerze(Long sablonaId) {
        if (!sablonaRepository.existsById(sablonaId)) {
            throw new IllegalArgumentException(zprava("chyba.sablona.neexistuje", sablonaId));
        }
        return sablonaVerzeRepository.findBySablonaIdOrderByUlozenoDneDesc(sablonaId);
    }

    public byte[] stahniVerzi(Long sablonaId, Long verzeId) throws IOException {
        return uloziste.nacti(najdiVerzi(sablonaId, verzeId).getNazevSouboru());
    }

    /**
     * Obnovi starsi verzi jako aktualni obsah sablony. Aktualni obsah se
     * pred prepsanim taky ulozi jako nova verze, aby se k nemu dalo pozdeji
     * vratit - obnoveni tedy nikdy nic trvale neztrati.
     *
     * Synchronized ze stejneho duvodu jako nahradSouborSablony vyse.
     */
    public synchronized void obnovVerzi(Long sablonaId, Long verzeId) throws IOException {
        Sablona sablona = Vyhledani.najdiNeboVyhod(sablonaRepository.findById(sablonaId), zprava("chyba.sablona.neexistuje", sablonaId));
        SablonaVerze verze = najdiVerzi(sablonaId, verzeId);
        byte[] obsahVerze = uloziste.nacti(verze.getNazevSouboru());

        ulozAktualniObsahJakoVerzi(sablona);

        uloziste.uloz(sablona.getNazevSouboru(), obsahVerze);
        sablona.oznacUpraveno();
        sablonaRepository.save(sablona);
    }

    private SablonaVerze najdiVerzi(Long sablonaId, Long verzeId) {
        SablonaVerze verze = Vyhledani.najdiNeboVyhod(sablonaVerzeRepository.findById(verzeId), zprava("chyba.sablona.verze_neexistuje", verzeId));
        if (!verze.getSablonaId().equals(sablonaId)) {
            throw new IllegalArgumentException(zprava("chyba.sablona.verze_neexistuje", verzeId));
        }
        return verze;
    }

    private void ulozAktualniObsahJakoVerzi(Sablona sablona) throws IOException {
        byte[] aktualniObsah = uloziste.nacti(sablona.getNazevSouboru());
        String nazevSouboruVerze = UUID.randomUUID() + ".docx";
        uloziste.uloz(nazevSouboruVerze, aktualniObsah);
        try {
            sablonaVerzeRepository.save(new SablonaVerze(sablona.getId(), nazevSouboruVerze));
        } catch (RuntimeException e) {
            // Kdyby DB zapis selhal az po ulozeni souboru na disk, soubor by tam
            // zustal navzdy jako neuklizeny "sirotek" bez jakekoliv reference -
            // smazeme ho, aby DB a disk zustaly v souladu.
            uloziste.smaz(nazevSouboruVerze);
            throw e;
        }
    }

    private void overPlatnyDocx(byte[] obsah) {
        try (XWPFDocument test = new XWPFDocument(new ByteArrayInputStream(obsah))) {
            // jen overeni, ze soubor jde otevrit jako platny .docx
        } catch (IOException | RuntimeException e) {
            if (jeZipBomba(e)) {
                throw new IllegalArgumentException(zprava("chyba.sablona.zip_bomba_upload"));
            }
            throw new IllegalArgumentException(zprava("chyba.sablona.nevalidni_docx"));
        }
    }

    // Apache POI hlida pomer komprese a max. velikost jednoho souboru v ZIPu
    // (nastaveno v PoiBezpecnostConfig) - pri prekroceni hodi vyjimku se
    // zpravou obsahujici "zip bomb". Tady na to jen reagujeme srozumitelnejsi
    // ceskou hlaskou misto obecne "poskozeny soubor".
    private boolean jeZipBomba(Throwable e) {
        String zprava = e.getMessage();
        return zprava != null && zprava.toLowerCase(Locale.ROOT).contains("zip bomb");
    }

    /** Sestavi regex, ktery na jeden zaber najde kterykoliv z placeholderu odpovidajicich klicum kontextu (${jmeno}, ${prijmeni}...). */
    private Pattern sestavVzorPlaceholderu(Set<String> nazvyPlaceholderu) {
        String alternativy = nazvyPlaceholderu.stream()
                .map(nazev -> Pattern.quote("${" + nazev + "}"))
                .collect(Collectors.joining("|"));
        return Pattern.compile(alternativy);
    }

    /** Odstrani obalku "${" a "}" z nalezeneho placeholderu, aby slo hodnotu dohledat v kontextu podle holeho nazvu. */
    private String holyNazevPlaceholderu(String placeholderSObalkou) {
        return placeholderSObalkou.substring(2, placeholderSObalkou.length() - 1);
    }

    /**
     * Konvence opakovani radku tabulky: radek, jehoz text obsahuje placeholder
     * s timhle prefixem, je "sablonovy" - misto nej se vlozi jedna kopie za
     * kazdou polozku ze seznamu. Zamerne obecne (funguje pro libovolnou sablonu
     * s touhle konvenci, ne jen pro fakturu) - viz zpracujTabulku.
     */
    private static final String PREFIX_POLOZKA = "${polozka.";

    /** Projde vsechny tabulky "tela" dokumentu (vcetne vnorenych) a v kazde zpracuje pripadny sablonovy radek. */
    private void zpracujTabulkyPolozek(IBody telo, List<Map<String, String>> polozky) {
        for (XWPFTable tabulka : telo.getTables()) {
            zpracujTabulku(tabulka, polozky);
            for (XWPFTableRow radek : tabulka.getRows()) {
                for (XWPFTableCell bunka : radek.getTableCells()) {
                    zpracujTabulkyPolozek(bunka, polozky);
                }
            }
        }
    }

    /**
     * Najde v tabulce sablonovy radek a nahradi ho N kopiemi (jednou za kazdou
     * polozku). Kopiruje se XML radku (CTRow.copy()), ne text - diky tomu
     * kopie zdedi formatovani bunek, sirky sloupcu i ohraniceni sablonoveho
     * radku. Tabulka bez sablonoveho radku (contains ${polozka. v zadne bunce)
     * zustane beze zmeny.
     */
    private void zpracujTabulku(XWPFTable tabulka, List<Map<String, String>> polozky) {
        int indexSablonovehoRadku = najdiIndexSablonovehoRadku(tabulka);
        if (indexSablonovehoRadku < 0) {
            return;
        }
        XWPFTableRow sablonovyRadek = tabulka.getRow(indexSablonovehoRadku);

        int index = indexSablonovehoRadku;
        for (Map<String, String> polozka : polozky) {
            CTRow kopie = (CTRow) sablonovyRadek.getCtRow().copy();
            XWPFTableRow novy = new XWPFTableRow(kopie, tabulka);
            // Nahrazeni MUSI probehnout pred addRow() - ta pri vkladani do XML
            // stromu tabulky (ctTbl) obsah radku znovu zkopiruje, takze uprava
            // provedena az na jiz vlozenem radku by se do vysledneho dokumentu
            // vubec nepropsala.
            nahradPolozkuVRadku(novy, polozka);
            tabulka.addRow(novy, index++);
        }

        // Puvodni sablonovy radek je po vlozeni kopii posunuty na pozici "index"
        // (za vsechny nove vlozene radky) - ted uz splnil svuj ucel a odstrani se.
        // Pri prazdnem seznamu polozek se index nezmenil, takze se odstrani rovnou on.
        tabulka.removeRow(index);
    }

    private int najdiIndexSablonovehoRadku(XWPFTable tabulka) {
        List<XWPFTableRow> radky = tabulka.getRows();
        for (int i = 0; i < radky.size(); i++) {
            if (obsahujeSablonovyPlaceholder(radky.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean obsahujeSablonovyPlaceholder(XWPFTableRow radek) {
        for (XWPFTableCell bunka : radek.getTableCells()) {
            if (bunka.getText() != null && bunka.getText().contains(PREFIX_POLOZKA)) {
                return true;
            }
        }
        return false;
    }

    /** Nahradi v jednom zkopirovanem radku placeholdery ${polozka.<klic>} hodnotami dane polozky. */
    private void nahradPolozkuVRadku(XWPFTableRow radek, Map<String, String> polozka) {
        Map<String, String> data = new LinkedHashMap<>();
        for (Map.Entry<String, String> zaznam : polozka.entrySet()) {
            data.put("polozka." + zaznam.getKey(), zaznam.getValue());
        }
        Pattern vzor = sestavVzorPlaceholderu(data.keySet());
        for (XWPFTableCell bunka : radek.getTableCells()) {
            nahradVTele(bunka, data, vzor);
        }
    }

    /**
     * Projde vsechny odstavce a tabulky jednoho "tela" dokumentu - samotneho
     * tela dokumentu, zahlavi, zapati, nebo bunky tabulky (vsechny implementuji
     * spolecne rozhrani IBody) - a nahradi v nich placeholdery. Tabulky
     * zanorene uvnitr bunky jine tabulky se resi rekurzi (bunka je taky IBody),
     * takze placeholder najde i v libovolne hluboko vnorene tabulce.
     */
    private void nahradVTele(IBody telo, Map<String, String> data, Pattern vzorPlaceholderu) {
        for (XWPFParagraph odstavec : telo.getParagraphs()) {
            nahradVOdstavci(odstavec, data, vzorPlaceholderu);
        }
        for (XWPFTable tabulka : telo.getTables()) {
            for (XWPFTableRow radek : tabulka.getRows()) {
                for (XWPFTableCell bunka : radek.getTableCells()) {
                    nahradVTele(bunka, data, vzorPlaceholderu);
                }
            }
        }
    }

    /**
     * Nahradi placeholdery v jednom odstavci.
     *
     * Word ma ošklivou vlastnost - text jednoho "viditelneho" radku casto
     * rozseka do vice XML runu (kvuli kontrole pravopisu, historii uprav apod.),
     * takze naivni hledani "${jmeno}" v jednom runu casto selze.
     * Reseni: vezmeme text CELEHO odstavce, provedeme nahrazeni na nem,
     * pak smazeme vsechny runy krome prvniho a do nej vlozime hotovy text.
     * Odstavec tim prijde o pripadne rozdilne formatovani uvnitr sebe sama
     * (napr. jen cast tucne), ale pro sablony s placeholdery to v naprove
     * vetsine pripadu nevadi.
     *
     * Nahrazeni probiha jednim pruchodem pres PUVODNI text pomoci regexu
     * (misto opakovaneho String.replace() v cyklu pro kazdy placeholder) -
     * kdyby totiz hodnota jednoho pole (napr. poznamka klienta) nahodou
     * obsahovala text vypadajici jako jiny placeholder, opakovane replace()
     * by ho omylem nahradil znovu. Pri jednom pruchodu se kazde misto v
     * puvodnim textu nahradi presne jednou.
     */
    private void nahradVOdstavci(XWPFParagraph odstavec, Map<String, String> data, Pattern vzorPlaceholderu) {
        String text = odstavec.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        Matcher shoda = vzorPlaceholderu.matcher(text);
        if (!shoda.find()) {
            return;
        }
        // Matcher.replaceAll(Function) bere navratovou hodnotu funkce jako "replacement"
        // retezec, ve kterem "$" a "\" maji specialni vyznam (odkaz na skupinu) - kdyby
        // hodnota pole klienta (napr. poznamka) nahodou obsahovala "$", je potreba ji
        // pred pouzitim jako nahrady oquotovat, jinak by appendReplacement() spadl.
        String novyText = shoda.replaceAll(vysledek -> Matcher.quoteReplacement(data.get(holyNazevPlaceholderu(vysledek.group()))));

        List<XWPFRun> runy = odstavec.getRuns();
        if (runy.isEmpty()) {
            return;
        }

        for (int i = runy.size() - 1; i >= 1; i--) {
            odstavec.removeRun(i);
        }
        runy.get(0).setText(novyText, 0);
    }
}
