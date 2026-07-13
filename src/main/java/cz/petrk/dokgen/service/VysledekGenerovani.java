package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Sablona;

/**
 * Vysledek DocumentGeneratorService.vygenerujDokument() - vyplnena sablona
 * jako .docx bajty spolu se Sablona zaznamem, ze ktereho vznikla. Volajici
 * (KlientController) tak nemusi sablonu podle id nacitat z databaze znovu,
 * jen aby zjistil jeji nazev pro nazev stahovaneho souboru a historii.
 */
public record VysledekGenerovani(byte[] obsah, Sablona sablona) {
}
