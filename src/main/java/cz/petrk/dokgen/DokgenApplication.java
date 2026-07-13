package cz.petrk.dokgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

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
        // udrzbova operace - nema smysl kvuli ni bootovat i webovy server.
        if (Arrays.stream(args).anyMatch(arg -> arg.startsWith("--zmenit-heslo"))) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }

        app.run(args);
    }

}
