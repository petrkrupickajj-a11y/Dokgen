package cz.petrk.dokgen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Tenka obalka nad JavaMailSender - odeslani nikdy nevyhodi vyjimku ven,
 * jen zaloguje chybu (viz konfigurace spring.mail.* v application.properties,
 * ktera bez nastavenych SMTP udaju odesilani stejne neumozni). Volajici
 * (napr. ResetHeslaService) tak muze uzivateli vzdy ukazat stejnou zpravu
 * bez ohledu na to, jestli se email doopravdy poslal.
 */
@Service
public class EmailOdesilatel {

    private static final Logger LOG = LoggerFactory.getLogger(EmailOdesilatel.class);

    private final JavaMailSender mailSender;
    private final String odesilatel;

    public EmailOdesilatel(JavaMailSender mailSender, @Value("${dokgen.mail.odesilatel}") String odesilatel) {
        this.mailSender = mailSender;
        this.odesilatel = odesilatel;
    }

    /** Vraci true, pokud se email podarilo odeslat - volajici (napr. ResetHeslaService) tak muze pri neuspechu nabidnout nahradni cestu. */
    public boolean odesli(String komu, String predmet, String telo) {
        try {
            SimpleMailMessage zprava = new SimpleMailMessage();
            zprava.setFrom(odesilatel);
            zprava.setTo(komu);
            zprava.setSubject(predmet);
            zprava.setText(telo);
            mailSender.send(zprava);
            return true;
        } catch (MailException e) {
            LOG.error("Nepodařilo se odeslat email uživateli \"{}\": {}", komu, e.getMessage());
            return false;
        }
    }
}
