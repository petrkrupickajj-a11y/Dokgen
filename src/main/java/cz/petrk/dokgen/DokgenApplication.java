package cz.petrk.dokgen;

import cz.petrk.dokgen.service.SplashOkno;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

// @EnableScheduling je potreba pro GenerovaneDokumentyUklidRunner (denni uklid
// starych vygenerovanych dokumentu z disku) - bez teto anotace by @Scheduled
// metody appka tise ignorovala.
@SpringBootApplication
@EnableScheduling
public class DokgenApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DokgenApplication.class);

        // Spring Boot by jinak samo nastavilo java.awt.headless=true (a to jeste
        // driv, nez appka stihne nacist application.properties, takze
        // spring.main.headless=false tam by neprislo vcas) - kvuli tlacitku
        // "Upravit" na /sablony (Desktop.getDesktop().open(...)) potrebujeme,
        // aby si SablonaUlozisteService overila realnou dostupnost GUI sama.
        app.setHeadless(false);

        // Zmena hesla pres konzoli (viz ZmenaHeslaRunner) je jednorazova
        // udrzbova operace - nema smysl kvuli ni bootovat i webovy server,
        // ani delat healthcheck/splash nize (ty se tykaji jen webove appky).
        if (Arrays.stream(args).anyMatch(arg -> arg.startsWith("--zmenit-heslo"))) {
            app.setWebApplicationType(WebApplicationType.NONE);
            app.run(args);
            return;
        }

        // Spusteni z .exe (viz sestavit-exe.bat) druhy a dalsikrat, dokud
        // predchozi instance jeste bezi na pozadi, by jinak skoncilo chybou
        // "Failed to launch JVM" (dve instance se peruji o stejny port).
        // Misto toho nejdriv zjistime, jestli appka uz odpovida na /zdravi -
        // pokud ano, jen otevreme prohlizec a Spring Boot vubec nestartujeme.
        String url = "http://localhost:" + zjistiPort() + "/";
        if (jeServerJizSpusteny(url + "zdravi")) {
            otevriProhlizec(url);
            return;
        }

        // Appka nema viditelnou konzoli (viz sestavit-exe.bat), takze bez
        // splash okna by uzivatel behem startu (muze trvat i desitky vterin)
        // nevidel zadnou zpetnou vazbu, ze se neco deje. Zavira ho
        // StartovaciProhlizecListener, jakmile appka nabehne.
        SplashOkno.zobraz();
        try {
            app.run(args);
        } catch (Exception e) {
            SplashOkno.zavri();
            ukazChybu("Server se nepodařilo spustit, zkuste to prosím znovu nebo restartujte počítač.");
            System.exit(1);
        }
    }

    /** Cte server.port stejnym zpusobem jako Spring Boot (system property, pak env var), s vychozi hodnotou 8080. */
    static String zjistiPort() {
        String sysProp = System.getProperty("server.port");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        String envVar = System.getenv("SERVER_PORT");
        if (envVar != null && !envVar.isBlank()) {
            return envVar;
        }
        return "8080";
    }

    /** Rychly (1s) HTTP dotaz na healthcheck - true, pokud uz na danem URL nekdo odpovida. */
    static boolean jeServerJizSpusteny(String zdraviUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(zdraviUrl))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void otevriProhlizec(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            // Appka uz bezi, jen se nepovedlo otevrit prohlizec - neni kriticke.
        }
    }

    private static void ukazChybu(String zprava) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(zprava);
            return;
        }
        JOptionPane.showMessageDialog(null, zprava, "Dokgen", JOptionPane.ERROR_MESSAGE);
    }

}
