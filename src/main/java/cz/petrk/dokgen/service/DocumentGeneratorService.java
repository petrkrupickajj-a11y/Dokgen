package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.SablonaVerze;
import cz.petrk.dokgen.entity.SmazanaVestavenaSablona;
import cz.petrk.dokgen.repository.SablonaRepository;
import cz.petrk.dokgen.repository.SablonaVerzeRepository;
import cz.petrk.dokgen.repository.SmazanaVestavenaSablonaRepository;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 *  3. Kde placeholder najdeme, nahradime ho skutecnou hodnotou z databaze.
 *  4. Vratime vysledny .docx jako pole bajtu -> to pak posleme uzivateli
 *     ke stazeni v controlleru.
 *
 * PRIDANI NOVE SABLONY se dnes dela primo v appce na strance /sablony
 * (nahranim .docx souboru s placeholdery ${jmeno}, ${prijmeni}, ${telefon},
 * ${email}, ${adresa}, ${mesto}, ${psc}, ${ico}, ${poznamka}, ${datum}).
 */
@Service
public class DocumentGeneratorService {

    private static final DateTimeFormatter FORMAT_DATA = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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

    public VysledekGenerovani vygenerujDokument(Klient klient, Long sablonaId) throws IOException {
        Sablona sablona = sablonaRepository.findById(sablonaId)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.neznama", sablonaId)));

        Map<String, String> data = sestavData(klient);
        Pattern vzorPlaceholderu = sestavVzorPlaceholderu(data.keySet());

        try (ByteArrayInputStream vstup = new ByteArrayInputStream(uloziste.nacti(sablona.getNazevSouboru()));
             XWPFDocument dokument = new XWPFDocument(vstup);
             ByteArrayOutputStream vystup = new ByteArrayOutputStream()) {

            // Telo dokumentu (odstavce i tabulky vcetne vnorenych - viz nahradVTele)
            nahradVTele(dokument, data, vzorPlaceholderu);

            // Zahlavi a zapati - dokument jich muze mit vic (pro prvni/lichou/sudou stranku)
            for (XWPFHeader zahlavi : dokument.getHeaderList()) {
                nahradVTele(zahlavi, data, vzorPlaceholderu);
            }
            for (XWPFFooter zapati : dokument.getFooterList()) {
                nahradVTele(zapati, data, vzorPlaceholderu);
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
        Sablona sablona = sablonaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.neexistuje", id)));

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
        Sablona sablona = sablonaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.neexistuje", id)));
        return uloziste.nacti(sablona.getNazevSouboru());
    }

    /**
     * Nahradi obsah existujici sablony (stejne id, stejny nazev) upravenou
     * verzi souboru - napr. po editaci ve Wordu/Google dokumentech a
     * zpetnem exportu do .docx. Funguje i pro vestavene sablony.
     */
    public void nahradSouborSablony(Long id, MultipartFile novySoubor) throws IOException {
        Sablona sablona = sablonaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.neexistuje", id)));

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
     */
    public void obnovVerzi(Long sablonaId, Long verzeId) throws IOException {
        Sablona sablona = sablonaRepository.findById(sablonaId)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.neexistuje", sablonaId)));
        SablonaVerze verze = najdiVerzi(sablonaId, verzeId);
        byte[] obsahVerze = uloziste.nacti(verze.getNazevSouboru());

        ulozAktualniObsahJakoVerzi(sablona);

        uloziste.uloz(sablona.getNazevSouboru(), obsahVerze);
        sablona.oznacUpraveno();
        sablonaRepository.save(sablona);
    }

    private SablonaVerze najdiVerzi(Long sablonaId, Long verzeId) {
        SablonaVerze verze = sablonaVerzeRepository.findById(verzeId)
                .orElseThrow(() -> new IllegalArgumentException(zprava("chyba.sablona.verze_neexistuje", verzeId)));
        if (!verze.getSablonaId().equals(sablonaId)) {
            throw new IllegalArgumentException(zprava("chyba.sablona.verze_neexistuje", verzeId));
        }
        return verze;
    }

    private void ulozAktualniObsahJakoVerzi(Sablona sablona) throws IOException {
        byte[] aktualniObsah = uloziste.nacti(sablona.getNazevSouboru());
        String nazevSouboruVerze = UUID.randomUUID() + ".docx";
        uloziste.uloz(nazevSouboruVerze, aktualniObsah);
        sablonaVerzeRepository.save(new SablonaVerze(sablona.getId(), nazevSouboruVerze));
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

    private Map<String, String> sestavData(Klient klient) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("${jmeno}", nullSafe(klient.getJmeno()));
        data.put("${prijmeni}", nullSafe(klient.getPrijmeni()));
        data.put("${telefon}", nullSafe(klient.getTelefon()));
        data.put("${email}", nullSafe(klient.getEmail()));
        data.put("${adresa}", nullSafe(klient.getAdresa()));
        data.put("${mesto}", nullSafe(klient.getMesto()));
        data.put("${psc}", nullSafe(klient.getPsc()));
        data.put("${ico}", nullSafe(klient.getIco()));
        data.put("${poznamka}", nullSafe(klient.getPoznamka()));
        data.put("${datum}", LocalDate.now().format(FORMAT_DATA));
        return data;
    }

    private String nullSafe(String hodnota) {
        return hodnota == null ? "" : hodnota;
    }

    /** Sestavi regex, ktery na jeden zaber najde kterykoliv ze znamych placeholderu (${jmeno}, ${prijmeni}...). */
    private Pattern sestavVzorPlaceholderu(Set<String> placeholdery) {
        String alternativy = placeholdery.stream().map(Pattern::quote).collect(Collectors.joining("|"));
        return Pattern.compile(alternativy);
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
        String novyText = shoda.replaceAll(vysledek -> Matcher.quoteReplacement(data.get(vysledek.group())));

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
