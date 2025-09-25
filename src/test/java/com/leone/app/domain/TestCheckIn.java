package com.leone.app.domain;

import org.junit.jupiter.api.*;
import java.io.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TestCheckIn {

    private static final String FILE_TEST = "CheckInTest.txt";

    @BeforeEach
    public void setUp() throws IOException {
        CheckIn.setFileName(FILE_TEST);
        // Reset del file di test
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_TEST))) {
            writer.write("// idCheckIn,idUtente,idPrenotazione,idPostazione,timestamp,metodo\n");
        }
    }

    @AfterEach
    public void tearDown() {
        // Ripristina il nome file originale
        CheckIn.setFileName("CheckIn.txt");
        File f = new File(FILE_TEST);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testCostruttoreEGetter() {
        CheckIn checkIn = new CheckIn(1, 100, 200, "QR");

        assertEquals(1, checkIn.getIdUtente());
        assertEquals(100, checkIn.getIdPrenotazione());
        assertEquals(200, checkIn.getIdPostazione());
        assertEquals("QR", checkIn.getMetodo());
    }

    /** Verifica che un check-in venga registrato correttamente su file */
    @Test
    public void testRegistraCheckIn() throws IOException {
        CheckIn checkIn = new CheckIn(1, 100, 200, "QR");
        assertTrue(checkIn.registraCheckIn()); // deve andare a buon fine

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_TEST))) {
            String line;
            boolean trovato = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("//") || line.isBlank()) continue;
                assertTrue(line.contains("QR"));
                assertTrue(line.contains("100")); // idPrenotazione
                trovato = true;
            }
            assertTrue(trovato, "Nessuna riga valida trovata nel file");
        }
    }

    /** Verifica che nextId venga incrementato leggendo il file */
    @Test
    public void testNextIdIncrementa() throws IOException {
        // Scriviamo una riga manuale nel file con id=5
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_TEST, true))) {
            writer.write("5,1,100,200," + LocalDateTime.now() + ",Manuale\n");
        }

        CheckIn checkIn = new CheckIn(2, 101, 201, "QR");
        assertTrue(checkIn.registraCheckIn());

        assertEquals(6, checkIn.getIdCheckIn()); // deve partire da maxId+1
    }

    /** Verifica che checkInGiaEffettuato ritorni true se già registrato */
    @Test
    public void testCheckInGiaEffettuatoTrue() {
        CheckIn checkIn = new CheckIn(1, 123, 456, "QR");
        assertTrue(checkIn.registraCheckIn());

        assertTrue(CheckIn.checkInGiaEffettuato(123));
    }

    /** Verifica che checkInGiaEffettuato ritorni false se non presente */
    @Test
    public void testCheckInGiaEffettuatoFalse() {
        assertFalse(CheckIn.checkInGiaEffettuato(999)); // non esiste nel file
    }

    /** Verifica che inizializzaNextIdDaFile riparta da 1 se file vuoto */
    @Test
    public void testNextIdFileVuoto() throws IOException {
        // reset file vuoto
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_TEST))) {
            writer.write("// file vuoto\n");
        }

        CheckIn.inizializzaNextIdDaFile();
        CheckIn checkIn = new CheckIn(1, 100, 200, "Manuale");
        assertTrue(checkIn.registraCheckIn());

        assertEquals(1, checkIn.getIdCheckIn()); // primo check-in
    }

    /** Verifica che il metodo toString contenga le info principali */
    @Test
    public void testToString() throws IOException {
        // Creo il check-in
        CheckIn checkIn = new CheckIn(10, 20, 30, "Manuale");

        // Registro il check-in così il timestamp viene impostato
        assertTrue(checkIn.registraCheckIn());

        // Ora posso testare il toString()
        String result = checkIn.toString();

        assertTrue(result.contains("idCheckIn=" + checkIn.getIdCheckIn()));
        assertTrue(result.contains("idUtente=" + checkIn.getIdUtente()));
        assertTrue(result.contains("idPrenotazione=" + checkIn.getIdPrenotazione()));
        assertTrue(result.contains("idPostazione=" + checkIn.getIdPostazione()));
        assertTrue(result.contains("timestamp=" + checkIn.getTimestamp().toString()));
        assertTrue(result.contains("metodo='" + checkIn.getMetodo() + "'"));
    }
}

