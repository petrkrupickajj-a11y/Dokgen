package cz.petrk.dokgen.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NazevSouboruTest {

    @Test
    void nullVstupVratiDokument() {
        assertThat(NazevSouboru.ocisti(null)).isEqualTo("dokument");
    }

    @Test
    void odstraniDiakritikuAPrevedeNaMalaPismena() {
        assertThat(NazevSouboru.ocisti("Cenová nabídka")).isEqualTo("cenova_nabidka");
    }

    @Test
    void specialniZnakySeNahradiPodtrzitkem() {
        assertThat(NazevSouboru.ocisti("Smlouva č. 1/2026 (finální)")).isEqualTo("smlouva_c_1_2026_finalni_");
    }

    @Test
    void vicenasobneSpecialniZnakySeSlouciDoJednohoPodtrzitka() {
        assertThat(NazevSouboru.ocisti("a---b   c")).isEqualTo("a_b_c");
    }

    @Test
    void prazdnyVstupVratiPrazdnyRetezec() {
        assertThat(NazevSouboru.ocisti("")).isEqualTo("");
    }

    @Test
    void cistyAlfanumerickyVstupZustaneBezZmenyJenMalymiPismeny() {
        assertThat(NazevSouboru.ocisti("Faktura2026")).isEqualTo("faktura2026");
    }
}
