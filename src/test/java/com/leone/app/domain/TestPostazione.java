package com.leone.app.domain;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileWriter;
import java.io.IOException;

public class TestPostazione {

    private static final String FILE_TEST = "PostazioniTest.txt";

    private Postazione postazioneConDotazione;
    private Postazione postazioneSenzaDotazione;
    private Postazione postazioneSoloId;
    private Area area;
    private Dotazione dotazione;

    @BeforeEach
    public void setUp() {
        // Usiamo file separato per i test
        Postazione.setFileName(FILE_TEST);
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write(""); // Pulizia file
        } catch (IOException e) {
            fail("Errore pulizia file di test: " + e.getMessage());
        }

        // Oggetti fittizi
        Sede sede = new Sede(1, "Sede Centrale", "Via Roma 1");
        area = new Area(10, "Biblioteca", "Area studio silenziosa", sede);
        dotazione = new Dotazione(100, "Monitor", "Monitor 24 pollici");

        postazioneConDotazione = new Postazione(1, "P01", "Vicino finestra", true, area, dotazione);
        postazioneSenzaDotazione = new Postazione(2, "P02", "Corridoio", false, area);
        postazioneSoloId = new Postazione(3);
    }

    @AfterEach
    public void tearDown() {
        postazioneConDotazione = null;
        postazioneSenzaDotazione = null;
        postazioneSoloId = null;
        area = null;
        dotazione = null;

        // Reset file name al default di produzione
        Postazione.setFileName("Postazioni.txt");
    }

    @Test
    public void testCostruttoreConDotazione() {
        assertEquals(1, postazioneConDotazione.getIdPostazione());
        assertEquals("P01", postazioneConDotazione.getCodice());
        assertEquals("Vicino finestra", postazioneConDotazione.getPosizione());
        assertTrue(postazioneConDotazione.isVicinanzaAFinestra());
        assertEquals(area, postazioneConDotazione.getArea());
        assertEquals(dotazione, postazioneConDotazione.getDotazione());
    }

    @Test
    public void testCostruttoreSenzaDotazione() {
        assertEquals(2, postazioneSenzaDotazione.getIdPostazione());
        assertEquals("P02", postazioneSenzaDotazione.getCodice());
        assertEquals("Corridoio", postazioneSenzaDotazione.getPosizione());
        assertFalse(postazioneSenzaDotazione.isVicinanzaAFinestra());
        assertEquals(area, postazioneSenzaDotazione.getArea());
        assertNull(postazioneSenzaDotazione.getDotazione());
    }

    @Test
    public void testCostruttoreSoloId() {
        assertEquals(3, postazioneSoloId.getIdPostazione());
        assertNull(postazioneSoloId.getCodice());
        assertNull(postazioneSoloId.getPosizione());
        assertFalse(postazioneSoloId.isVicinanzaAFinestra());
        assertNull(postazioneSoloId.getArea());
        assertNull(postazioneSoloId.getDotazione());
    }

    @Test
    public void testSetter() {
        postazioneConDotazione.setCodice("PX");
        postazioneConDotazione.setPosizione("Angolo");
        postazioneConDotazione.setVicinanzaAFinestra(false);

        assertEquals("PX", postazioneConDotazione.getCodice());
        assertEquals("Angolo", postazioneConDotazione.getPosizione());
        assertFalse(postazioneConDotazione.isVicinanzaAFinestra());
    }

    @Test
    public void testToString() {
        String result = postazioneConDotazione.toString();
        assertTrue(result.contains("P01"));
        assertTrue(result.contains("Vicino finestra"));
        assertTrue(result.contains("Biblioteca"));
        assertTrue(result.contains("Monitor"));
    }

    @Test
    public void testGetByIdFound() {
        // Scriviamo riga nel file
        String fakeData = "1,P01,Vicino finestra,true,10,100\n";
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write(fakeData);
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }

        Postazione result = Postazione.getById(1);
        assertNotNull(result);
        assertEquals(1, result.getIdPostazione());
        assertEquals("P01", result.getCodice());
        assertEquals("Vicino finestra", result.getPosizione());
        assertTrue(result.isVicinanzaAFinestra());
        assertNotNull(result.getArea());
        assertEquals(10, result.getArea().getIdArea());
    }

    @Test
    public void testGetByIdNotFound() {
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write("2,P02,Corridoio,false,10,100\n");
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }

        Postazione result = Postazione.getById(999);
        assertNull(result);
    }
}

