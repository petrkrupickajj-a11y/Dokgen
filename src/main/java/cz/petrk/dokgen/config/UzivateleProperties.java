package cz.petrk.dokgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Seznam prihlasovacich uctu appky (viz application.properties, dokgen.uzivatele[N].email / [N].heslo). */
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
        private String email;
        private String heslo;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getHeslo() {
            return heslo;
        }

        public void setHeslo(String heslo) {
            this.heslo = heslo;
        }
    }
}
