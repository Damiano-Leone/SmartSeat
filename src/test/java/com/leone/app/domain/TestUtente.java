package com.leone.app.domain;

import org.junit.jupiter.api.*;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtente {

    private Utente utente;
    private static final String FILE_TEST = "UtentiTest.txt";

    @BeforeEach
    public void setUp() {
        // Imposta il file di test
        Utente.setFileName(FILE_TEST);

        // Svuota il file prima di ogni test
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write("");
        } catch (IOException e) {
            fail("Errore nella pulizia file di test: " + e.getMessage());
        }

        // Inizializza un utente di test
        utente = new Utente(1, "Mario", "Rossi", "mario.rossi@example.com", "utente");
    }

    @AfterEach
    public void tearDown() {
        utente = null;
        Utente.setFileName("Utenti.txt"); // ripristina file di produzione
    }

    // helper per scrivere righe nel file di test
    private void scriviFileTest(String contenuto) {
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write(contenuto);
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }
    }

    @Test
    public void testCostruttoreEGetter() {
        assertEquals(1, utente.getIdUtente());
        assertEquals("Mario", utente.getNome());
        assertEquals("Rossi", utente.getCognome());
        assertEquals("mario.rossi@example.com", utente.getEmail());
        assertEquals("utente", utente.getRuolo());
    }

    @Test
    public void testCostruttoreSoloId() {
        Utente u2 = new Utente(2);
        assertEquals(2, u2.getIdUtente());
        assertNull(u2.getNome());
        assertNull(u2.getCognome());
        assertNull(u2.getEmail());
        assertNull(u2.getRuolo());
    }

    @Test
    public void testSetter() {
        utente.setNome("Luca");
        utente.setCognome("Bianchi");
        utente.setEmail("luca.bianchi@example.com");
        utente.setRuolo("admin");

        assertEquals("Luca", utente.getNome());
        assertEquals("Bianchi", utente.getCognome());
        assertEquals("luca.bianchi@example.com", utente.getEmail());
        assertEquals("admin", utente.getRuolo());
    }

    @Test
    public void testToString() {
        String result = utente.toString();
        assertTrue(result.contains("Mario"));
        assertTrue(result.contains("Rossi"));
        assertTrue(result.contains("mario.rossi@example.com"));
        assertTrue(result.contains("utente"));
    }

    @Test
    public void testGetById() {
        String fakeData = "1,Mario,Rossi,mario.rossi@example.com,utente\n";
        scriviFileTest(fakeData);

        Utente result = Utente.getById(1);
        assertNotNull(result);
        assertEquals(1, result.getIdUtente());
        assertEquals("Mario", result.getNome());
        assertEquals("Rossi", result.getCognome());
        assertEquals("mario.rossi@example.com", result.getEmail());
        assertEquals("utente", result.getRuolo());
    }

    @Test
    public void testGetByIdNotFound() {
        String fakeData = "2,Luca,Bianchi,luca.bianchi@example.com,admin\n";
        scriviFileTest(fakeData);

        Utente result = Utente.getById(999);
        assertNull(result);
    }
}

