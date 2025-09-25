package com.leone.app.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TestNotifica {

    // Sostituire con l'indirizzo email su cui si desidera ricevere le notifiche durante i test
    private static final String EMAIL_TEST = "test@example.com";


    @Test
    void testInvioNotificaPrenotazione() {
        Prenotazione prenotazione = creaPrenotazioneFake();
        String qrPath = "src/test/resources/qr_test.png"; // file dummy allegato

        assertDoesNotThrow(() -> {
            Notifica.generaNotificaPrenotazione(prenotazione, qrPath, false);
        }, "L'invio della notifica prenotazione non deve generare eccezioni");
    }

    @Test
    void testInvioNotificaAnnullamentoSenzaRegola() {
        // Annullamento per Check-In non effettuato
        Prenotazione prenotazione = creaPrenotazioneFake();

        assertDoesNotThrow(() -> {
            Notifica.generaNotificaAnnullamento(prenotazione, null, false);
        }, "L'invio della notifica annullamento (senza regola) non deve generare eccezioni");
    }

    @Test
    void testInvioNotificaAnnullamentoConLimiteOreAttivo() {
        // Annullamento per regola applicata
        Prenotazione prenotazione = creaPrenotazioneFake();
        LocalDate dataPrenotazione = prenotazione.getData();

        Regola regola = new Regola(
                2,
                "Superato limite ore giornaliere",
                Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.GLOBALE,
                -1, // nessun riferimento specifico
                "8", // limite massimo 8 ore
                dataPrenotazione.minusDays(1), // regola attiva dal giorno prima
                dataPrenotazione.plusDays(1),  // e valida anche il giorno dopo
                "Regola che limita le ore prenotabili per giornata"
        );

        assertDoesNotThrow(() -> {
            Notifica.generaNotificaAnnullamento(prenotazione, regola, false);
        }, "L'invio della notifica con regola attiva non deve generare eccezioni");
    }

    private Prenotazione creaPrenotazioneFake() {
        // Utente di test
        Utente utente = new Utente(
                1,
                "Mario",
                "Rossi",
                EMAIL_TEST,
                "utente"
        );

        // Sede di test
        Sede sede = new Sede(1, "Sede Centrale", "Via Roma 1");

        // Area collegata alla sede
        Area area = new Area(1, "Open Space", "Area comune con scrivanie condivise", sede);

        // Dotazione di test
        Dotazione dotazione = new Dotazione(1, "Monitor", "Monitor 27 pollici");

        // Postazione collegata ad area + dotazione
        Postazione postazione = new Postazione(
                1,
                "P01",
                "Primo piano - vicino alla finestra",
                true,
                area,
                dotazione
        );

        // Prenotazione:
        Prenotazione prenotazione = new Prenotazione(
                1,
                LocalDate.now().plusDays(1),   // data futura
                "09:00-11:00",
                utente,
                postazione
        );

        // Aggiungo la sede alla prenotazione
        prenotazione.setSede(sede);

        return prenotazione;
    }
}
