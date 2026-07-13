package cz.petrk.dokgen.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Prevede jiz vygenerovany .docx dokument (s dosazenymi hodnotami klienta)
 * do PDF. Jde o jednoduche vykresleni textu odstavcu a bunek tabulek -
 * nezachovava puvodni formatovani Wordu (tucne, ramecky tabulek...), ale
 * nepotrebuje zadny externi nastroj (LibreOffice apod.), jen cistou Javu
 * (Apache PDFBox).
 *
 * Font DejaVu Sans je zabaleny primo v resources/fonts a vlozeny do PDF,
 * protoze standardni PDF fonty (Helvetica...) nemaji v kodovani ceskou
 * diakritiku (č, ř, š, ž...) a vykreslovani by na ni spadlo.
 */
@Service
public class PdfExportService {

    private static final float OKRAJ = 50;
    private static final float VELIKOST_PISMA = 11;
    private static final float RADKOVANI = 15;
    private static final PDRectangle FORMAT_STRANKY = PDRectangle.A4;

    private final MessageSource zpravy;

    public PdfExportService(MessageSource zpravy) {
        this.zpravy = zpravy;
    }

    public byte[] prevedNaPdf(byte[] docxObsah) throws IOException {
        List<String> radky = nactiRadky(docxObsah);

        try (PDDocument pdf = new PDDocument()) {
            PDFont font;
            try (InputStream fontStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
                font = PDType0Font.load(pdf, fontStream);
            }

            // Stranka drzi otevreny PDPageContentStream - i kdyby cyklus niz
            // spadl na vyjimce (napr. znak, ktery font neumi), musi se ten
            // rozpracovany content stream pred opustenim metody zavrit.
            Stranka stranka = null;
            try {
                stranka = novaStranka(pdf, font);
                for (String radek : radky) {
                    for (String zalomenyRadek : zalomText(radek, font, VELIKOST_PISMA, FORMAT_STRANKY.getWidth() - 2 * OKRAJ)) {
                        if (stranka.y <= OKRAJ) {
                            stranka.zavri();
                            stranka = null;
                            stranka = novaStranka(pdf, font);
                        }
                        stranka.obsah.showText(zalomenyRadek.isEmpty() ? " " : zalomenyRadek);
                        stranka.obsah.newLineAtOffset(0, -RADKOVANI);
                        stranka.y -= RADKOVANI;
                    }
                }
            } finally {
                if (stranka != null) {
                    stranka.zavri();
                }
            }

            ByteArrayOutputStream vystup = new ByteArrayOutputStream();
            pdf.save(vystup);
            return vystup.toByteArray();
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException(zpravy.getMessage("chyba.pdf.prevod_selhal", null, LocaleContextHolder.getLocale()), e);
        }
    }

    private List<String> nactiRadky(byte[] docxObsah) throws IOException {
        List<String> radky = new ArrayList<>();
        try (XWPFDocument dokument = new XWPFDocument(new ByteArrayInputStream(docxObsah))) {
            for (XWPFParagraph odstavec : dokument.getParagraphs()) {
                radky.add(odstavec.getText());
            }
            for (XWPFTable tabulka : dokument.getTables()) {
                for (XWPFTableRow radek : tabulka.getRows()) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFTableCell bunka : radek.getTableCells()) {
                        if (sb.length() > 0) {
                            sb.append("   |   ");
                        }
                        sb.append(bunka.getText());
                    }
                    radky.add(sb.toString());
                }
            }
        }
        return radky;
    }

    private Stranka novaStranka(PDDocument pdf, PDFont font) throws IOException {
        PDPage page = new PDPage(FORMAT_STRANKY);
        pdf.addPage(page);
        PDPageContentStream obsah = new PDPageContentStream(pdf, page);
        obsah.setFont(font, VELIKOST_PISMA);
        obsah.beginText();
        float y = FORMAT_STRANKY.getHeight() - OKRAJ;
        obsah.newLineAtOffset(OKRAJ, y);
        return new Stranka(obsah, y);
    }

    private List<String> zalomText(String text, PDFont font, float velikost, float maxSirka) throws IOException {
        List<String> vysledek = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            vysledek.add("");
            return vysledek;
        }

        StringBuilder radek = new StringBuilder();
        for (String slovo : text.split(" ")) {
            for (String cast : zalomDlouheSlovo(slovo, font, velikost, maxSirka)) {
                String zkusRadek = radek.isEmpty() ? cast : radek + " " + cast;
                if (sirkaTextu(zkusRadek, font, velikost) > maxSirka && !radek.isEmpty()) {
                    vysledek.add(radek.toString());
                    radek = new StringBuilder(cast);
                } else {
                    radek = new StringBuilder(zkusRadek);
                }
            }
        }
        vysledek.add(radek.toString());
        return vysledek;
    }

    /**
     * Rozdeli jedno "slovo" (bez mezer) na kousky, ktere se kazdy sam o sobe
     * vejde do maxSirka. Bez tohohle by napr. dlouhe URL nebo email v poznamce
     * klienta, ktery nema kam se zalomit na mezere, jednoduse pretekl za pravy
     * okraj stranky.
     */
    private List<String> zalomDlouheSlovo(String slovo, PDFont font, float velikost, float maxSirka) throws IOException {
        if (sirkaTextu(slovo, font, velikost) <= maxSirka) {
            return List.of(slovo);
        }

        List<String> kousky = new ArrayList<>();
        StringBuilder kousek = new StringBuilder();
        for (int i = 0; i < slovo.length(); i++) {
            char znak = slovo.charAt(i);
            String zkusKousek = kousek.toString() + znak;
            if (sirkaTextu(zkusKousek, font, velikost) > maxSirka && !kousek.isEmpty()) {
                kousky.add(kousek.toString());
                kousek = new StringBuilder().append(znak);
            } else {
                kousek.append(znak);
            }
        }
        if (!kousek.isEmpty()) {
            kousky.add(kousek.toString());
        }
        return kousky;
    }

    private float sirkaTextu(String text, PDFont font, float velikost) throws IOException {
        return font.getStringWidth(text) / 1000 * velikost;
    }

    /** Drzi otevreny content stream jedne stranky a aktualni Y pozici kurzoru. */
    private static final class Stranka {
        private final PDPageContentStream obsah;
        private float y;

        private Stranka(PDPageContentStream obsah, float y) {
            this.obsah = obsah;
            this.y = y;
        }

        private void zavri() throws IOException {
            obsah.endText();
            obsah.close();
        }
    }
}
