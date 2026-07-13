package cz.petrk.dokgen.tools;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * Jednorazovy generator .docx sablon pro word-templates/.
 *
 * Nahrazuje puvodni generuj_sablony.js (Node.js + knihovna "docx") -
 * projekt ma byt cely v Jave, takze se stejnym vysledkem (dve .docx
 * sablony s placeholdery ${jmeno}, ${prijmeni}, ... - viz
 * DocumentGeneratorService) tady pouzivame Apache POI, ktery uz je
 * v projektu jako zavislost pro samotne generovani dokumentu klientum.
 *
 * SPUSTENI (po "mvnw compile"):
 *   java -cp target/classes cz.petrk.dokgen.tools.SablonyGenerator
 *
 * Skript se spousti rucne jen kdyz potrebujes sablony pretvorit od nuly -
 * beznou pravni/textovou uz pravu je jednodussi udelat primo v hotovem
 * .docx souboru ve Wordu.
 */
public final class SablonyGenerator {

    private static final int OKRAJ_NAHORU_DOLU = 1000;
    private static final int OKRAJ_VLEVO_VPRAVO = 1200;
    private static final String SEDA_VYPLN = "F2F2F2";

    private SablonyGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path cilovaSlozka = Path.of("src/main/resources/word-templates");
        Files.createDirectories(cilovaSlozka);

        ulozDokument(vytvorSmlouvu(), cilovaSlozka.resolve("smlouva.docx"));
        ulozDokument(vytvorNabidku(), cilovaSlozka.resolve("nabidka.docx"));
        ulozDokument(vytvorFakturu(), cilovaSlozka.resolve("faktura.docx"));
        ulozDokument(vytvorProtokol(), cilovaSlozka.resolve("protokol.docx"));
        ulozDokument(vytvorPlnouMoc(), cilovaSlozka.resolve("plna_moc.docx"));

        System.out.println("Hotovo: sablony vygenerovany.");
    }

    // ---------- SABLONA 1: Smlouva o poskytovani sluzeb ----------

    private static XWPFDocument vytvorSmlouvu() {
        XWPFDocument dokument = new XWPFDocument();
        nastavOkraje(dokument);

        titulek(dokument, "SMLOUVA O POSKYTOVÁNÍ SLUŽEB");
        kurzivaNaStred(dokument, "uzavřená dne ${datum} podle příslušných ustanovení občanského zákoníku", 400);

        nadpisH2(dokument, "Smluvní strany", 200, 150);

        tucnaVeta(dokument, "Poskytovatel:", 80);
        odstavec(dokument, "Coreforge, se sídlem Praha, Česká republika", 200);

        tucnaVeta(dokument, "Objednatel:", 80);
        odstavec(dokument, "Jméno a příjmení: ${jmeno} ${prijmeni}");
        odstavec(dokument, "Adresa: ${adresa}, ${psc} ${mesto}");
        odstavec(dokument, "IČO: ${ico}");
        odstavec(dokument, "Telefon: ${telefon}");
        odstavec(dokument, "Email: ${email}", 200);

        nadpisH2(dokument, "1. Předmět smlouvy", 200, 150);
        odstavec(dokument, "Poskytovatel se zavazuje pro objednatele zajistit sjednané služby v rozsahu "
                + "a za podmínek dále specifikovaných v této smlouvě, a to s odbornou péčí a v dohodnutých "
                + "termínech.", 200);

        nadpisH2(dokument, "2. Doba trvání a platební podmínky", 200, 150);
        odstavec(dokument, "Tato smlouva se uzavírá na dobu neurčitou s výpovědní lhůtou 1 měsíc. Platební "
                + "podmínky jsou upraveny samostatnou přílohou nebo cenovou nabídkou.", 200);

        nadpisH2(dokument, "3. Poznámka", 200, 150);
        odstavec(dokument, "${poznamka}", 400);

        nadpisH2(dokument, "Podpisy smluvních stran", 300, 300);

        XWPFTable tabulka = dokument.createTable(1, 2);
        odeberOhraniceni(tabulka);
        XWPFTableRow radek = tabulka.getRow(0);
        naplnBunkuPodpisem(radek.getCell(0), "Poskytovatel");
        naplnBunkuPodpisem(radek.getCell(1), "Objednatel (${jmeno} ${prijmeni})");

        return dokument;
    }

    private static void naplnBunkuPodpisem(XWPFTableCell bunka, String popisek) {
        odstavecVBunce(bunka, "…………………………………………");
        odstavecVBunce(bunka, popisek);
    }

    // ---------- SABLONA 2: Cenova nabidka ----------

    private static XWPFDocument vytvorNabidku() {
        XWPFDocument dokument = new XWPFDocument();
        nastavOkraje(dokument);

        titulek(dokument, "CENOVÁ NABÍDKA");
        kurzivaNaStred(dokument, "vystaveno dne ${datum}", 400);

        nadpisH2(dokument, "Odběratel", 100, 150);
        odstavec(dokument, "${jmeno} ${prijmeni}");
        odstavec(dokument, "${adresa}, ${psc} ${mesto}");
        odstavec(dokument, "IČO: ${ico}");
        odstavec(dokument, "Telefon: ${telefon}");
        odstavec(dokument, "Email: ${email}", 300);

        nadpisH2(dokument, "Přehled nabízených služeb", 100, 150);

        XWPFTable tabulka = dokument.createTable(2, 2);
        XWPFTableRow hlavicka = tabulka.getRow(0);
        naplnHlavickovouBunku(hlavicka.getCell(0), "Položka");
        naplnHlavickovouBunku(hlavicka.getCell(1), "Cena");

        XWPFTableRow radek = tabulka.getRow(1);
        odstavecVBunce(radek.getCell(0), "Doplnit podle nabídky");
        odstavecVBunce(radek.getCell(1), "—");

        nadpisH2(dokument, "Poznámka", 300, 150);
        odstavec(dokument, "${poznamka}", 200);

        kurzivaNaStred(dokument, "Nabídka je platná 30 dní od data vystavení.", 0);

        return dokument;
    }

    private static void naplnHlavickovouBunku(XWPFTableCell bunka, String text) {
        bunka.setColor(SEDA_VYPLN);
        XWPFParagraph odstavec = bunka.getParagraphs().get(0);
        XWPFRun run = odstavec.createRun();
        run.setText(text);
        run.setBold(true);
    }

    // ---------- SABLONA 3: Faktura ----------

    private static XWPFDocument vytvorFakturu() {
        XWPFDocument dokument = new XWPFDocument();
        nastavOkraje(dokument);

        titulek(dokument, "FAKTURA – DAŇOVÝ DOKLAD");
        kurzivaNaStred(dokument, "vystavena dne ${datum}", 400);

        nadpisH2(dokument, "Údaje faktury", 100, 150);
        odstavec(dokument, "Číslo faktury: Doplnit");
        odstavec(dokument, "Datum vystavení: ${datum}");
        odstavec(dokument, "Datum splatnosti: Doplnit", 200);

        nadpisH2(dokument, "Dodavatel", 200, 150);
        odstavec(dokument, "Coreforge, se sídlem Praha, Česká republika", 200);

        nadpisH2(dokument, "Odběratel", 200, 150);
        odstavec(dokument, "${jmeno} ${prijmeni}");
        odstavec(dokument, "${adresa}, ${psc} ${mesto}");
        odstavec(dokument, "IČO: ${ico}");
        odstavec(dokument, "Telefon: ${telefon}");
        odstavec(dokument, "Email: ${email}", 200);

        nadpisH2(dokument, "Fakturované položky", 200, 150);

        XWPFTable tabulka = dokument.createTable(2, 4);
        XWPFTableRow hlavicka = tabulka.getRow(0);
        naplnHlavickovouBunku(hlavicka.getCell(0), "Položka");
        naplnHlavickovouBunku(hlavicka.getCell(1), "Množství");
        naplnHlavickovouBunku(hlavicka.getCell(2), "Cena za jednotku");
        naplnHlavickovouBunku(hlavicka.getCell(3), "Celkem");

        XWPFTableRow radek = tabulka.getRow(1);
        odstavecVBunce(radek.getCell(0), "Doplnit podle skutečnosti");
        odstavecVBunce(radek.getCell(1), "—");
        odstavecVBunce(radek.getCell(2), "—");
        odstavecVBunce(radek.getCell(3), "—");

        tucnaVeta(dokument, "Celkem k úhradě: Doplnit Kč", 300);

        nadpisH2(dokument, "Platební údaje", 100, 150);
        odstavec(dokument, "Číslo účtu: Doplnit");
        odstavec(dokument, "Variabilní symbol: Doplnit", 200);

        nadpisH2(dokument, "Poznámka", 200, 150);
        odstavec(dokument, "${poznamka}", 300);

        kurzivaNaStred(dokument, "Fakturu prosím uhraďte do data splatnosti uvedeného výše.", 0);

        return dokument;
    }

    // ---------- SABLONA 4: Protokol o predani ----------

    private static XWPFDocument vytvorProtokol() {
        XWPFDocument dokument = new XWPFDocument();
        nastavOkraje(dokument);

        titulek(dokument, "PROTOKOL O PŘEDÁNÍ");
        kurzivaNaStred(dokument, "sepsáno dne ${datum}", 400);

        nadpisH2(dokument, "Smluvní strany", 200, 150);

        tucnaVeta(dokument, "Předávající:", 80);
        odstavec(dokument, "Coreforge, se sídlem Praha, Česká republika", 200);

        tucnaVeta(dokument, "Přebírající:", 80);
        odstavec(dokument, "Jméno a příjmení: ${jmeno} ${prijmeni}");
        odstavec(dokument, "Adresa: ${adresa}, ${psc} ${mesto}");
        odstavec(dokument, "IČO: ${ico}");
        odstavec(dokument, "Telefon: ${telefon}");
        odstavec(dokument, "Email: ${email}", 200);

        nadpisH2(dokument, "Předmět předání", 200, 150);
        odstavec(dokument, "${poznamka}", 200);

        nadpisH2(dokument, "Stav při předání", 200, 150);
        odstavec(dokument, "Doplnit popis stavu předávaného předmětu/díla.", 400);

        nadpisH2(dokument, "Podpisy", 300, 300);

        XWPFTable tabulka = dokument.createTable(1, 2);
        odeberOhraniceni(tabulka);
        XWPFTableRow radek = tabulka.getRow(0);
        naplnBunkuPodpisem(radek.getCell(0), "Předávající");
        naplnBunkuPodpisem(radek.getCell(1), "Přebírající (${jmeno} ${prijmeni})");

        return dokument;
    }

    // ---------- SABLONA 5: Plna moc ----------

    private static XWPFDocument vytvorPlnouMoc() {
        XWPFDocument dokument = new XWPFDocument();
        nastavOkraje(dokument);

        titulek(dokument, "PLNÁ MOC");
        kurzivaNaStred(dokument, "vystavena dne ${datum}", 400);

        nadpisH2(dokument, "Zmocnitel", 100, 150);
        odstavec(dokument, "Jméno a příjmení: ${jmeno} ${prijmeni}");
        odstavec(dokument, "Adresa: ${adresa}, ${psc} ${mesto}");
        odstavec(dokument, "IČO: ${ico}", 200);

        nadpisH2(dokument, "Zmocněnec", 200, 150);
        odstavec(dokument, "Jméno a příjmení: Doplnit");
        odstavec(dokument, "Adresa: Doplnit", 200);

        nadpisH2(dokument, "Rozsah zmocnění", 200, 150);
        odstavec(dokument, "${poznamka}", 300);

        odstavec(dokument, "Tato plná moc je platná od ${datum} do jejího odvolání zmocnitelem, "
                + "není-li stanoveno jinak.", 300);

        nadpisH2(dokument, "Podpisy", 300, 300);

        XWPFTable tabulka = dokument.createTable(1, 2);
        odeberOhraniceni(tabulka);
        XWPFTableRow radek = tabulka.getRow(0);
        naplnBunkuPodpisem(radek.getCell(0), "Zmocnitel (${jmeno} ${prijmeni})");
        naplnBunkuPodpisem(radek.getCell(1), "Zmocněnec");

        return dokument;
    }

    // ---------- Spolecne pomocne metody ----------

    private static void nastavOkraje(XWPFDocument dokument) {
        CTSectPr sectPr = dokument.getDocument().getBody().isSetSectPr()
                ? dokument.getDocument().getBody().getSectPr()
                : dokument.getDocument().getBody().addNewSectPr();
        CTPageMar okraje = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        okraje.setTop(BigInteger.valueOf(OKRAJ_NAHORU_DOLU));
        okraje.setBottom(BigInteger.valueOf(OKRAJ_NAHORU_DOLU));
        okraje.setLeft(BigInteger.valueOf(OKRAJ_VLEVO_VPRAVO));
        okraje.setRight(BigInteger.valueOf(OKRAJ_VLEVO_VPRAVO));
    }

    private static void titulek(XWPFDocument dokument, String text) {
        XWPFParagraph odstavec = dokument.createParagraph();
        odstavec.setAlignment(ParagraphAlignment.CENTER);
        odstavec.setSpacingAfter(300);
        XWPFRun run = odstavec.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(24);
    }

    private static void kurzivaNaStred(XWPFDocument dokument, String text, int mezeraPo) {
        XWPFParagraph odstavec = dokument.createParagraph();
        odstavec.setAlignment(ParagraphAlignment.CENTER);
        if (mezeraPo > 0) {
            odstavec.setSpacingAfter(mezeraPo);
        }
        XWPFRun run = odstavec.createRun();
        run.setText(text);
        run.setItalic(true);
        run.setFontSize(10);
    }

    private static void nadpisH2(XWPFDocument dokument, String text, int mezeraPred, int mezeraPo) {
        XWPFParagraph odstavec = dokument.createParagraph();
        odstavec.setSpacingBefore(mezeraPred);
        odstavec.setSpacingAfter(mezeraPo);
        XWPFRun run = odstavec.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(13);
    }

    private static void tucnaVeta(XWPFDocument dokument, String text, int mezeraPo) {
        XWPFParagraph odstavec = dokument.createParagraph();
        odstavec.setSpacingAfter(mezeraPo);
        XWPFRun run = odstavec.createRun();
        run.setText(text);
        run.setBold(true);
    }

    private static void odstavec(XWPFDocument dokument, String text) {
        odstavec(dokument, text, 0);
    }

    private static void odstavec(XWPFDocument dokument, String text, int mezeraPo) {
        XWPFParagraph odstavec = dokument.createParagraph();
        if (mezeraPo > 0) {
            odstavec.setSpacingAfter(mezeraPo);
        }
        odstavec.createRun().setText(text);
    }

    // Nova bunka ma vzdy jeden prazdny odstavec - prvni volani ho vyuzije,
    // dalsi volani na stejne bunce prida novy radek pod nej.
    private static void odstavecVBunce(XWPFTableCell bunka, String text) {
        XWPFParagraph odstavec = bunka.getParagraphs().get(0);
        if (!odstavec.getRuns().isEmpty()) {
            odstavec = bunka.addParagraph();
        }
        odstavec.createRun().setText(text);
    }

    private static void odeberOhraniceni(XWPFTable tabulka) {
        tabulka.setTopBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        tabulka.setBottomBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        tabulka.setLeftBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        tabulka.setRightBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        tabulka.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
        tabulka.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "auto");
    }

    private static void ulozDokument(XWPFDocument dokument, Path cesta) throws IOException {
        try (dokument; OutputStream vystup = Files.newOutputStream(cesta)) {
            dokument.write(vystup);
        }
    }
}
