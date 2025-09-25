package com.leone.app.domain;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestDotazione {

    private Dotazione dotazione;

    @BeforeEach
    public void setUp() {
        dotazione = new Dotazione(1, "Monitor", "Monitor 24 pollici");
    }

    @AfterEach
    public void tearDown() {
        dotazione = null;
    }

    @Test
    public void testCostruttoreEGetter() {
        assertEquals(1, dotazione.getIdDotazione());
        assertEquals("Monitor", dotazione.getNome());
        assertEquals("Monitor 24 pollici", dotazione.getDescrizione());
    }

    @Test
    public void testSetter() {
        dotazione.setNome("Tastiera");
        dotazione.setDescrizione("Tastiera meccanica");

        assertEquals("Tastiera", dotazione.getNome());
        assertEquals("Tastiera meccanica", dotazione.getDescrizione());
    }

    @Test
    public void testToString() {
        String result = dotazione.toString();
        assertTrue(result.contains("1"));
        assertTrue(result.contains("Monitor"));
        assertTrue(result.contains("Monitor 24 pollici"));
    }
}
