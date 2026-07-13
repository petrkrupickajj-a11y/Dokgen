package cz.petrk.dokgen;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class DokgenApplicationTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("server.port");
    }

    @Test
    void zjistiPortVraciVychoziHodnotuKdyzNicNeniNastaveno() {
        assertThat(DokgenApplication.zjistiPort()).isEqualTo("8080");
    }

    @Test
    void zjistiPortUpredostniSystemovouVlastnostPredVychozi() {
        System.setProperty("server.port", "9090");

        assertThat(DokgenApplication.zjistiPort()).isEqualTo("9090");
    }

    @Test
    void jeServerJizSpustenyVraciTrueKdyzNekdoOdpovidaNaUrl() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/zdravi", vymena -> {
            byte[] telo = "OK".getBytes();
            vymena.sendResponseHeaders(200, telo.length);
            vymena.getResponseBody().write(telo);
            vymena.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/zdravi";

            assertThat(DokgenApplication.jeServerJizSpusteny(url)).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void jeServerJizSpustenyVraciFalseKdyzNicNeposloucha() throws IOException {
        // Port ziskame od kratce beziciho serveru, hned ho zase zastavime -
        // tim mame port, na kterem uz garantovane nikdo neposloucha.
        HttpServer docasnyServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        int volnyPort = docasnyServer.getAddress().getPort();
        docasnyServer.stop(0);

        String url = "http://localhost:" + volnyPort + "/zdravi";

        assertThat(DokgenApplication.jeServerJizSpusteny(url)).isFalse();
    }
}
