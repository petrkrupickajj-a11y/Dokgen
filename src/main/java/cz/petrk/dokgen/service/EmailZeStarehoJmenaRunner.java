package cz.petrk.dokgen.service;

import cz.petrk.dokgen.util.EmailValidace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Jednorazova migrace uctu zalozenych jeste v dobe, kdy se appka prihlasovala
 * uzivatelskym jmenem (sloupec JMENO), ne emailem. Pri prechodu na prihlasovani
 * emailem se stare ucty nepreklopily - zustaly v databazi s EMAIL = NULL, takze
 * je DokgenUserDetailsService (findByEmail) uz nikdy nenajde. Vysledek: majitel
 * takoveho uctu se nemuze prihlasit, i kdyz zna spravne heslo, a nepomuze mu ani
 * zapomenute heslo (ResetHeslaService hleda ucet taky podle emailu).
 *
 * Runner proto u uctu s prazdnym emailem zkusi pouzit hodnotu ze sloupce JMENO -
 * pokud v nem je platny email (typicky se lide uzivatelskym jmenem prihlasovali
 * prave svym emailem), preklopi ho do sloupce EMAIL. Heslo (BCrypt hash) zustava
 * beze zmeny, takze uzivatel se rovnou prihlasi svym puvodnim heslem.
 *
 * Co runner ZAMERNE nedela:
 * - nesaha na ucty, jejichz JMENO neni email (napr. "admin", "recenzent") - nelze
 *   z nich odvodit, komu patri; jen je vypise do logu, at se rozhodne rucne,
 * - nic neprepisuje ani nemaze pri kolizi s uz existujicim uctem (napr. kdyz mezitim
 *   vznikl novy ucet se stejnym emailem) - taky jen zaloguje.
 *
 * Bezi pred ostatnimi runnery (@Order), aby uz EmailNormalizaceRunner videl ucty
 * s vyplnenym emailem.
 *
 * Sloupec JMENO neni soucasti entity Uzivatel (v aktualnim modelu uz neexistuje),
 * proto se cte primym SQL dotazem. Kdyz v databazi neni (cista nova instalace),
 * runner jen tise skonci.
 */
@Component
@Order(0)
public class EmailZeStarehoJmenaRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(EmailZeStarehoJmenaRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public EmailZeStarehoJmenaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!sloupecJmenoExistuje()) {
            return;
        }

        List<Map<String, Object>> kMigraci = jdbcTemplate.queryForList(
                "SELECT ID, JMENO FROM UZIVATEL WHERE EMAIL IS NULL AND JMENO IS NOT NULL");

        for (Map<String, Object> radek : kMigraci) {
            Long id = ((Number) radek.get("ID")).longValue();
            String email = EmailValidace.normalizuj((String) radek.get("JMENO"));

            if (!EmailValidace.jePlatny(email)) {
                LOG.warn("""
                        Účet s id {} má staré přihlašovací jméno "{}", které není email - nechávám ho beze změny.
                        Přihlásit se pod ním nepůjde. Pokud ten účet ještě potřebuješ, založ si nový přes /registrace.
                        """, id, radek.get("JMENO"));
                continue;
            }

            if (emailJeObsazeny(email)) {
                LOG.warn("""
                        Účet s id {} by měl po migraci email "{}", ten už ale patří jinému účtu - nechávám ho beze změny.
                        Ověř prosím ručně, který z těch dvou účtů má zůstat.
                        """, id, email);
                continue;
            }

            jdbcTemplate.update("UPDATE UZIVATEL SET EMAIL = ? WHERE ID = ?", email, id);
            LOG.info("Účtu s id {} jsem doplnil email \"{}\" ze starého přihlašovacího jména - "
                    + "přihlas se jím a svým původním heslem.", id, email);
        }
    }

    private boolean sloupecJmenoExistuje() {
        Integer pocet = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'UZIVATEL' "
                        + "AND UPPER(COLUMN_NAME) = 'JMENO'",
                Integer.class);
        return pocet != null && pocet > 0;
    }

    private boolean emailJeObsazeny(String email) {
        Integer pocet = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM UZIVATEL WHERE EMAIL = ?", Integer.class, email);
        return pocet != null && pocet > 0;
    }
}
