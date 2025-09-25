package com.leone.app.util;

import com.leone.app.domain.CheckIn;
import com.leone.app.domain.Postazione;
import com.leone.app.domain.Prenotazione;
import com.leone.app.domain.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TestPrenotazioneScheduler {

    // Sostituire con l'indirizzo email su cui si desidera ricevere le notifiche durante i test
    private static final String EMAIL_TEST = "test@example.com";

    private PrenotazioneScheduler scheduler;
    private Prenotazione prenotazione;
    private static final String FILE_TEST = "PrenotazioniTest.txt";
    private static final String FILE_TEST_CheckIn = "CheckInTest.txt";

    @BeforeEach
    public void setUp() throws IOException {
        // Imposta file test
        Prenotazione.setFileName(FILE_TEST);
        CheckIn.setFileName(FILE_TEST_CheckIn);

        // Crea scheduler
        scheduler = new PrenotazioneScheduler();

        // Crea prenotazione fittizia
        prenotazione = new Prenotazione(
                1,
                LocalDate.now().minusDays(2), // già scaduta per simulare annullamento
                "09:00-10:00",
                new Utente(1, "Test", "User", EMAIL_TEST, "utente"),
                new Postazione(1, "P01", "Posizione test", false, null, null)
        );

        // Registra prenotazione nel file di test
        prenotazione.registraPrenotazione();
    }

    @AfterEach
    public void tearDown() {
        // Ripristina file di default
        Prenotazione.setFileName("Prenotazioni.txt");
        CheckIn.setFileName("CheckIn.txt");

        // Svuota il file di test per lasciare l'ambiente pulito
        try (FileWriter fw = new FileWriter(FILE_TEST, false)) {
            // scrive niente → file svuotato
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter fwCheckIn = new FileWriter(FILE_TEST_CheckIn, false)) {
            // svuota anche il file CheckIn
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testRunAvviaControlloPrenotazioni() {
        assertDoesNotThrow(() -> scheduler.run(),
                "L'esecuzione del ciclo di controllo prenotazioni non deve generare eccezioni");
    }
}

