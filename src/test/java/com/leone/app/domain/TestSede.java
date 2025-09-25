package com.leone.app.domain;

import org.junit.jupiter.api.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestSede {

    private Sede sede;
    private Area area;

    private static final String FILE_TEST = "SediTest.txt";

    @BeforeEach
    public void setUp() {
        // Imposta il file di test
        Sede.setFileName(FILE_TEST);

        // Svuota il file all'inizio di ogni test
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write("");
        } catch (IOException e) {
            fail("Errore nella pulizia file di test: " + e.getMessage());
        }

        // Inizializza oggetti
        sede = new Sede(1, "Sede Centrale", "Via Roma 1, Roma");
        area = new Area(10, "Biblioteca", "Area studio silenziosa", sede);
        sede.addArea(area);
    }

    @AfterEach
    public void tearDown() {
        sede = null;
        area = null;

        // Ripristina file di produzione
        Sede.setFileName("Sedi.txt");
    }

    // Helper per scrivere dati nel file di test
    private void scriviFileTest(String contenuto) {
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write(contenuto);
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }
    }

    @Test
    public void testCostruttoreEGetter() {
        assertEquals(1, sede.getIdSede());
        assertEquals("Sede Centrale", sede.getNome());
        assertEquals("Via Roma 1, Roma", sede.getIndirizzo());
    }

    @Test
    public void testSetter() {
        sede.setNome("Sede Nord");
        sede.setIndirizzo("Via Milano 10");

        assertEquals("Sede Nord", sede.getNome());
        assertEquals("Via Milano 10", sede.getIndirizzo());
    }

    @Test
    public void testAddArea() {
        Area a2 = new Area(20, "Laboratorio", "Area con PC", sede);
        sede.addArea(a2);

        Map<Integer, Area> elenco = sede.getElencoAree();
        assertEquals(2, elenco.size());
        assertTrue(elenco.containsKey(10));
        assertTrue(elenco.containsKey(20));
    }

    @Test
    public void testGetElencoAree() {
        Map<Integer, Area> elenco = sede.getElencoAree();

        assertEquals(1, elenco.size());
        assertTrue(elenco.containsKey(10));

        Area a = elenco.get(10);
        assertNotNull(a);
        assertEquals(10, a.getIdArea());
        assertEquals("Biblioteca", a.getNome());
        assertEquals("Area studio silenziosa", a.getDescrizione());
        assertEquals(sede, a.getSede()); // verifica che l'area punti alla stessa sede
    }

    @Test
    public void testGetPostazioni() {
        Dotazione dot = new Dotazione(1, "Monitor", "Monitor 24 pollici");

        Postazione p1 = new Postazione(1, "P01", "Finestra", true, area, dot);
        Postazione p2 = new Postazione(2, "P02", "Corridoio", false, area, dot);

        area.addPostazione(p1);
        area.addPostazione(p2);

        List<Postazione> postazioni = sede.getPostazioni();
        assertEquals(2, postazioni.size());
        assertTrue(postazioni.contains(p1));
        assertTrue(postazioni.contains(p2));
    }

    @Test
    public void testToString() {
        String str = sede.toString();
        assertTrue(str.contains("Sede Centrale"));
        assertTrue(str.contains("Via Roma 1, Roma"));
    }

    @Test
    public void testGetById() {
        // Scrive dati sul file di test
        String fakeData = "1,\"Sede Centrale\",\"Via Roma 1, Roma\"\n";
        scriviFileTest(fakeData);

        Sede result = Sede.getById(1);
        assertNotNull(result);
        assertEquals(1, result.getIdSede());
        assertEquals("Sede Centrale", result.getNome());
        assertEquals("Via Roma 1, Roma", result.getIndirizzo());
    }

    @Test
    public void testGetByIdNotFound() {
        String fakeData = "2,\"Sede Sud\",\"Via Napoli 5\"\n";
        scriviFileTest(fakeData);

        Sede result = Sede.getById(999);
        assertNull(result);
    }
}
