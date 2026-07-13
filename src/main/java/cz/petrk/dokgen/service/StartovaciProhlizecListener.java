package cz.petrk.dokgen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Po startu appky automaticky otevre vychozi prohlizec na hlavni stranku, aby
 * appka pusobila jako normalni desktopovy program (viz .exe balicek pres
 * jpackage), ne jako neco, co je potreba obsluhovat pres konzoli. Na headless
 * serveru (bez GUI) se preskoci - stejne duvod jako u tlacitka "Upravit" na
 * /sablony (viz SablonaUlozisteService).
 */
@Component
public class StartovaciProhlizecListener {

    private final boolean otevrit;
    private final String url;

    public StartovaciProhlizecListener(@Value("${dokgen.otevrit-prohlizec-po-startu:true}") boolean otevrit,
                                        @Value("${server.port:8080}") String port) {
        this.otevrit = otevrit;
        this.url = "http://localhost:" + port + "/";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void otevriProhlizec() {
        if (!otevrit || !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            // Otevreni prohlizece neni kriticke pro beh appky - server uz jede,
            // uzivatel si stranku otevre rucne.
        }
    }
}
