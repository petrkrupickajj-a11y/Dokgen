package cz.petrk.dokgen.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jednoduchy healthcheck endpoint - overuje, jestli appka uz bezi. Pouziva
 * ho DokgenApplication.main() pred spustenim druhe instance z .exe, aby
 * se predeslo konfliktu portu ("Failed to launch JVM") - misto dalsiho
 * pokusu o start appka jen zjisti, ze uz bezi, a otevre prohlizec.
 */
@RestController
public class ZdraviController {

    @GetMapping("/zdravi")
    public ResponseEntity<String> zdravi() {
        return ResponseEntity.ok("OK");
    }
}
