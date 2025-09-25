package com.leone.app.domain;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

public class TestArea {

    private Area area;
    private Sede sede;

    @BeforeEach
    public void setUp() {
        // Inizializza file di test
        Area.setFileName("AreeTest.txt");
        try (FileWriter fw = new FileWriter("AreeTest.txt")) {
            fw.write(""); // Pulizia del file prima di ogni test
        } catch (IOException e) {
            fail("Errore nella pulizia file test: " + e.getMessage());
        }

        // Inizializza una sede fittizia
        sede = new Sede(1, "Sede Centrale", "Via Roma 1, Roma");

        // Inizializza area di test
        area = new Area(10, "Biblioteca", "Area studio silenziosa", sede);
    }

    @AfterEach
    public void tearDown() {
        // Reset oggetti
        area = null;
        sede = null;

        // Reset file al default di produzione
        Area.setFileName("Aree.txt");
    }

    @Test
    public void testCostruttoreEGetter() {
        assertEquals(10, area.getIdArea());
        assertEquals("Biblioteca", area.getNome());
        assertEquals("Area studio silenziosa", area.getDescrizione());
        assertEquals(sede, area.getSede());
    }

    @Test
    public void testSetter() {
        area.setNome("Laboratorio");
        area.setDescrizione("Area con computer");

        assertEquals("Laboratorio", area.getNome());
        assertEquals("Area con computer", area.getDescrizione());
        assertNotEquals("Biblioteca", area.getNome());
    }

    @Test
    public void testAddPostazione() {
        Dotazione dotazione = new Dotazione(1, "Monitor", "Monitor 24 pollici");

        Postazione p1 = new Postazione(1, "P01", "Finestra", true, area, dotazione);
        Postazione p2 = new Postazione(2, "P02", "Corridoio", false, area, dotazione);

        area.addPostazione(p1);
        area.addPostazione(p2);

        Map<Integer, Postazione> elenco = area.getElencoPostazioni();

        assertEquals(2, elenco.size());
        assertTrue(elenco.containsKey(1));
        assertTrue(elenco.containsKey(2));
        assertEquals("P01", elenco.get(1).getCodice());
        assertTrue(elenco.get(1).isVicinanzaAFinestra());
        assertFalse(elenco.get(2).isVicinanzaAFinestra());
    }


    @Test
    public void testToString() {
        String result = area.toString();
        assertTrue(result.contains("Biblioteca"));
        assertTrue(result.contains("Area studio silenziosa"));
        assertTrue(result.contains("Sede Centrale"));
    }

    @Test
    public void testGetById() {
        // Simula file AreeTest.txt
        String fakeData = "10,Biblioteca,Area studio silenziosa,1\n";
        try (FileWriter fw = new FileWriter("AreeTest.txt")) {
            fw.write(fakeData);
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }

        Area result = Area.getById(10);
        assertNotNull(result);
        assertEquals(10, result.getIdArea());
        assertEquals("Biblioteca", result.getNome());
    }

    @Test
    public void testGetByIdNotFound() {
        try (FileWriter fw = new FileWriter("AreeTest.txt")) {
            fw.write("20,Laboratorio,PC disponibili,1\n");
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }

        Area result = Area.getById(999);
        assertNull(result);
    }
}

