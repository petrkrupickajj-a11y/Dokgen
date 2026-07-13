package cz.petrk.dokgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Seznam prihlasovacich uctu appky (viz application.properties,
 * dokgen.uzivatele[N].jmeno / [N].heslo / [N].role). Role urcuje opravneni
 * (ADMIN/ASISTENTKA, viz entita Role a SecurityConfig) - kdyz neni u uctu
 * nastavena, UzivateleSeeder mu prideli ADMIN (zpetne kompatibilni chovani
 * pro pripad, ze nekdo appku upgraduje ze starsi verze bez rolí).
 */
@ConfigurationProperties(prefix = "dokgen")
public class UzivateleProperties {

    private List<Ucet> uzivatele = new ArrayList<>();

    public List<Ucet> getUzivatele() {
        return uzivatele;
    }

    public void setUzivatele(List<Ucet> uzivatele) {
        this.uzivatele = uzivatele;
    }

    public static class Ucet {
        private String jmeno;
        private String heslo;
        private String role;

        public String getJmeno() {
            return jmeno;
        }

        public void setJmeno(String jmeno) {
            this.jmeno = jmeno;
        }

        public String getHeslo() {
            return heslo;
        }

        public void setHeslo(String heslo) {
            this.heslo = heslo;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
