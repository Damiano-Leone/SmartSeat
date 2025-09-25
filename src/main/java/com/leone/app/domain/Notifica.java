package com.leone.app.domain;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

import com.leone.app.util.LogManager;
import io.github.cdimascio.dotenv.Dotenv;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Notifica {

    private static final Dotenv dotenv = Dotenv.load();
    private static final Logger logger = LogManager.getLogger();

    public static void generaNotificaAnnullamento(Prenotazione prenotazione, Regola regola, boolean daScheduler) {
        String to = prenotazione.getUtente().getEmail();
        String subject = "⚠️ Prenotazione annullata (ID: " + prenotazione.getId() + ")";

        String messaggioRegola;
        String dettagliRegola = "";
        if (regola == null) {
            messaggioRegola = "La tua prenotazione è scaduta perché non è stato effettuato il check-in entro i 15 minuti previsti.";
        } else {
            switch (regola.getTipo()) {
                case BLOCCO_UTENTE:
                    messaggioRegola = "La tua prenotazione è stata annullata perché l'utente è temporaneamente bloccato.";
                    break;
                case BLOCCO_GIORNI:
                    messaggioRegola = "La tua prenotazione è stata annullata perché non sono consentite prenotazioni in questi giorni.";
                    break;
                case LIMITE_ORE_GIORNALIERO:
                    messaggioRegola = "La tua prenotazione è stata annullata perché è stato superato il limite di ore consentito.";
                    break;
                default:
                    messaggioRegola = "La tua prenotazione è stata annullata a causa di una regola applicata.";
            }
            dettagliRegola = "<p><b>Dettagli regola applicata:</b></p>"
                    + "<ul>"
                    + "<li><b>Descrizione:</b> " + regola.getDescrizione() + "</li>"
                    + "<li><b>Data inizio:</b> " + regola.getDataInizio() + "</li>"
                    + "<li><b>Data fine:</b> " + regola.getDataFine() + "</li>"
                    + "</ul>";
        }

        String bodyHtml = "<h2>Ciao " + prenotazione.getUtente().getNome() + " " + prenotazione.getUtente().getCognome() + ",</h2>"
                + "<p><b>Attenzione:</b> " + messaggioRegola + "</p>"
                + "<p><b>Dettagli prenotazione annullata:</b></p><ul>"
                + "<li><b>ID prenotazione:</b> " + prenotazione.getId() + "</li>"
                + "<li><b>Data:</b> " + prenotazione.getData() + "</li>"
                + "<li><b>Fascia oraria:</b> " + prenotazione.getFasciaOraria() + "</li>"
                + "<li><b>Postazione:</b> " + prenotazione.getPostazione().getCodice()
                + " (" + prenotazione.getPostazione().getPosizione() + ")</li></ul>"
                + dettagliRegola
                + "<p>Ti invitiamo a verificare le regole applicate e riprovare a prenotare in modo alternativo o quando sarà possibile.</p>"
                + "<p>Saluti,<br>Il sistema prenotazioni</p>";

        inviaEmail(to, subject, bodyHtml, null, daScheduler);
    }

    public static void generaNotificaPrenotazione(Prenotazione prenotazione, String qrPath, boolean daScheduler) {
        String to = prenotazione.getUtente().getEmail();
        String subject = "✅ Conferma prenotazione (ID: " + prenotazione.getId() + ")";

        String bodyHtml = "<h2>Ciao " + prenotazione.getUtente().getNome() + " " + prenotazione.getUtente().getCognome() + ",</h2>"
                + "<p>La tua prenotazione è stata registrata con successo:</p>"
                + "<ul>"
                + "<li><b>ID prenotazione:</b> " + prenotazione.getId() + "</li>"
                + "<li><b>Data:</b> " + prenotazione.getData() + "</li>"
                + "<li><b>Fascia oraria:</b> " + prenotazione.getFasciaOraria() + "</li>"
                + "<li><b>Postazione:</b> " + prenotazione.getPostazione().getCodice()
                + " (" + prenotazione.getPostazione().getPosizione() + ")</li>"
                + "<li><b>Sede:</b> " + prenotazione.getSede().getNome()
                + " - " + prenotazione.getSede().getIndirizzo() + "</li>"
                + "<li><b>Vicino finestra:</b> " + (prenotazione.isVicinoFinestra() ? "Sì" : "No") + "</li>"
                + "<li><b>Area:</b> " + prenotazione.getPostazione().getArea().getNome() + "</li>"
                + "<li><b>Dotazione:</b> " + prenotazione.getPostazione().getDotazione().getNome()
                + " - " + prenotazione.getPostazione().getDotazione().getDescrizione() + "</li>"
                + "<li><b>Regole applicate:</b><ul>";

        for (Regola r : prenotazione.getRegoleApplicate()) {
            bodyHtml += "<li><b>Descrizione:</b> " + r.getDescrizione() + "</li>"
                    + "<li><b>Data inizio:</b> " + r.getDataInizio() + "</li>"
                    + "<li><b>Data fine:</b> " + r.getDataFine() + "</li>";
        }

        bodyHtml += "</ul></li>"
                + "</ul>"
                + "<p>In allegato trovi il QR code della prenotazione.</p>"
                + "<p>Saluti,<br>Il sistema prenotazioni</p>";


        // Allegato QR code
        inviaEmail(to, subject, bodyHtml, List.of(qrPath), daScheduler);
    }

    private static void inviaEmail(String to, String subject, String bodyHtml, List<String> allegati, boolean daScheduler) {
        String host = dotenv.get("SMTP_HOST");
        String port = dotenv.get("SMTP_PORT");
        String user = dotenv.get("SMTP_USER");
        String pass = dotenv.get("SMTP_PASS");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            Message message = new MimeMessage(session);
            try {
                message.setFrom(new InternetAddress(user, "SmartSeat"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                message.setFrom(new InternetAddress(user)); // fallback solo email
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Corpo HTML
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(bodyHtml, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Allegati (es. QR code)
            if (allegati != null) {
                for (String filePath : allegati) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(filePath);
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(source.getName());
                    multipart.addBodyPart(attachmentPart);
                }
            }

            message.setContent(multipart);
            Transport.send(message);

            if (daScheduler) {
                logger.info("📧 Email inviata con successo a " + to);
            } else {
                System.out.println("📧 Email inviata con successo a " + to);
            }

        } catch (MessagingException e) {
            if (daScheduler) {
                logger.severe("❌ Errore nell'invio email a " + to + ": " + e.getMessage());
            } else {
                e.printStackTrace();
                System.out.println("❌ Errore nell'invio email a " + to);
            }
        }
    }
}

