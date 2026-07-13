package cz.petrk.dokgen.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PdfExportServiceTest {

    private final PdfExportService service = new PdfExportService(new StaticMessageSource());

    @Test
    void prevedNaPdfZachovaBezneOdstavceSDiakritikou() throws IOException {
        byte[] pdf = service.prevedNaPdf(docxSTextem("Toto je běžná věta s diakritikou."));

        assertThat(extrahujText(pdf)).contains("Toto je běžná věta s diakritikou.");
    }

    /**
     * Regresni test na opravu zalomText()/zalomDlouheSlovo() - dlouhy retezec
     * bez jedine mezery (typicky URL nebo email v poznamce klienta) se drive
     * nezalomil vubec a pretekl za pravy okraj stranky. Ted se rozdeli na vice
     * radku, aniz by se ztratil jediny znak.
     */
    @Test
    void prevedNaPdfZalomiPrilisDlouheSlovoBezMezerNaViceRadku() throws IOException {
        String dlouheSlovo = "a".repeat(400);
        byte[] pdf = service.prevedNaPdf(docxSTextem(dlouheSlovo));

        String extrahovanyText = extrahujText(pdf).replaceAll("\\s", "");
        assertThat(extrahovanyText).isEqualTo(dlouheSlovo);
    }

    private byte[] docxSTextem(String text) throws IOException {
        try (XWPFDocument dokument = new XWPFDocument()) {
            XWPFRun run = dokument.createParagraph().createRun();
            run.setText(text);
            ByteArrayOutputStream vystup = new ByteArrayOutputStream();
            dokument.write(vystup);
            return vystup.toByteArray();
        }
    }

    private String extrahujText(byte[] pdf) throws IOException {
        try (PDDocument dokument = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(dokument);
        }
    }
}
