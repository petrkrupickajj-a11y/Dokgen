package cz.petrk.dokgen.service;

import cz.petrk.dokgen.entity.Klient;
import cz.petrk.dokgen.entity.Sablona;
import cz.petrk.dokgen.entity.VygenerovanyDokument;
import cz.petrk.dokgen.repository.VygenerovanyDokumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class HistorieServiceTest {

    private VygenerovanyDokumentRepository repository;
    private HistorieService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(VygenerovanyDokumentRepository.class);
        service = new HistorieService(repository);
    }

    @Test
    void zaznamenejUlozimZaznamSeJmenemKlientaASablony() {
        Klient klient = new Klient();
        klient.setJmeno("Jan");
        klient.setPrijmeni("Novák");
        Sablona sablona = new Sablona("Smlouva", "smlouva.docx", true);

        service.zaznamenej(klient, sablona, "PDF");

        ArgumentCaptor<VygenerovanyDokument> zachyceny = ArgumentCaptor.forClass(VygenerovanyDokument.class);
        Mockito.verify(repository).save(zachyceny.capture());
        assertThat(zachyceny.getValue().getKlientJmenoPrijmeni()).isEqualTo("Jan Novák");
        assertThat(zachyceny.getValue().getSablonaNazev()).isEqualTo("Smlouva");
        assertThat(zachyceny.getValue().getFormat()).isEqualTo("PDF");
    }

    @Test
    void vyhledejVratiStrankyVeSpravneVelikostiAPoctu() {
        given(repository.findAllByOrderByVytvorenoDneDesc()).willReturn(vytvorZaznamy(25));

        List<VygenerovanyDokument> prvniStrana = service.vyhledej(null, null, null, 0);
        List<VygenerovanyDokument> druhaStrana = service.vyhledej(null, null, null, 1);

        assertThat(prvniStrana).hasSize(HistorieService.VELIKOST_STRANKY);
        assertThat(druhaStrana).hasSize(25 - HistorieService.VELIKOST_STRANKY);
        assertThat(service.celkemStranek(null, null, null)).isEqualTo(2);
    }

    @Test
    void vyhledejStrankaMimoRozsahVratiPrazdnySeznam() {
        given(repository.findAllByOrderByVytvorenoDneDesc()).willReturn(vytvorZaznamy(5));

        assertThat(service.vyhledej(null, null, null, 5)).isEmpty();
    }

    @Test
    void vyhledejFiltrujePodleJmenaKlienta() {
        List<VygenerovanyDokument> zaznamy = List.of(
                new VygenerovanyDokument(1L, "Petr Krupička", "Smlouva", "WORD"),
                new VygenerovanyDokument(2L, "Jana Nováková", "Faktura", "PDF"));
        given(repository.findAllByOrderByVytvorenoDneDesc()).willReturn(zaznamy);

        List<VygenerovanyDokument> vysledek = service.vyhledej(null, null, "krupič", 0);

        assertThat(vysledek).hasSize(1);
        assertThat(vysledek.get(0).getKlientJmenoPrijmeni()).isEqualTo("Petr Krupička");
    }

    @Test
    void vyhledejFiltrujePodleMesiceARoku() throws Exception {
        VygenerovanyDokument lednovy = new VygenerovanyDokument(1L, "Klient Leden", "Smlouva", "WORD");
        nastavDatum(lednovy, LocalDateTime.of(2025, 1, 15, 10, 0));

        VygenerovanyDokument breznovy = new VygenerovanyDokument(2L, "Klient Březen", "Smlouva", "WORD");
        nastavDatum(breznovy, LocalDateTime.of(2025, 3, 15, 10, 0));

        given(repository.findAllByOrderByVytvorenoDneDesc()).willReturn(List.of(lednovy, breznovy));

        List<VygenerovanyDokument> vysledek = service.vyhledej(2025, 1, null, 0);

        assertThat(vysledek).hasSize(1);
        assertThat(vysledek.get(0).getKlientJmenoPrijmeni()).isEqualTo("Klient Leden");
    }

    private List<VygenerovanyDokument> vytvorZaznamy(int pocet) {
        List<VygenerovanyDokument> zaznamy = new ArrayList<>();
        for (int i = 0; i < pocet; i++) {
            zaznamy.add(new VygenerovanyDokument((long) i, "Klient " + i, "Smlouva", "WORD"));
        }
        return zaznamy;
    }

    private void nastavDatum(VygenerovanyDokument dokument, LocalDateTime datum) throws Exception {
        Field pole = VygenerovanyDokument.class.getDeclaredField("vytvorenoDne");
        pole.setAccessible(true);
        pole.set(dokument, datum);
    }
}
