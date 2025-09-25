package com.leone.app.domain;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

public class TestRegola {

    private static final String FILE_TEST = "RegoleTest.txt";
    private static final String FILE_PRENOTAZIONI_TEST = "PrenotazioniTest.txt";

    private Regola regolaSede;
    private Regola regolaUtente;
    private Regola regolaGlobale;
    private Utente utente;
    private Sede sede;
    private Area area;
    private Dotazione dotazione;
    private Postazione postazione;

    @BeforeEach
    public void setUp() throws IOException {
        // Pulizia file test prenotazioni
        try (FileWriter fw = new FileWriter(FILE_PRENOTAZIONI_TEST)) {
            fw.write("// id,postazione,data,fascia,idUtente,uuid\n");
        }

        // Pulizia file test regole
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write("// ID;NOME;TIPO;SCOPETIPO;ID_RIFERIMENTO;VALORE;DATA_INIZIO;DATA_FINE;DESCRIZIONE\n");
        }

        Prenotazione.setFileName(FILE_PRENOTAZIONI_TEST);
        Regola.setFileName(FILE_TEST);

        utente = new Utente(1, "Mario", "Rossi", "mario@test.com", "USER");
        sede = new Sede(1, "Sede Centrale", "Via Roma");
        area = new Area(1, "OpenSpace", "Area principale di lavoro", sede);
        dotazione = new Dotazione(1, "PC", "Portatile");
        postazione = new Postazione(1, "A1", "Piano Terra", true, area, dotazione);

        LocalDate oggi = LocalDate.now();
        LocalDate domani = oggi.plusDays(1);

        regolaSede = new Regola(-1, "Blocco Sede", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.SEDE, 1, "VAL1", oggi, domani, "Regola di test sede");

        regolaUtente = new Regola(-1, "Blocco Utente", Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.UTENTE, 10, "8", oggi, domani, "Max 8 ore");

        regolaGlobale = new Regola(-1, "Blocco Globale", Regola.TipoRegola.BLOCCO_GIORNI,
                Regola.ScopeRegola.GLOBALE, -1, "LUNEDI,MARTEDI", oggi, domani, "Blocca giorni");
    }

    @AfterEach
    public void tearDown() {
        // Ripristino file di default
        Regola.setFileName("Regole.txt");
        Prenotazione.setFileName("Prenotazioni.txt");

        // Pulisce i riferimenti agli oggetti
        utente = null;
        sede = null;
        area = null;
        dotazione = null;
        postazione = null;
        regolaSede = null;
        regolaUtente = null;
        regolaGlobale = null;

        // Elimina i file di test se esistono
        File fPren = new File(FILE_PRENOTAZIONI_TEST);
        if (fPren.exists()) {
            fPren.delete();
        }

        File fRegole = new File(FILE_TEST);
        if (fRegole.exists()) {
            fRegole.delete();
        }
    }

    @Test
    public void testCostruttoreEGetter() {
        assertEquals("Blocco Sede", regolaSede.getNome());
        assertEquals(Regola.TipoRegola.BLOCCO_UTENTE, regolaSede.getTipo());
        assertEquals(Regola.ScopeRegola.SEDE, regolaSede.getScope());
        assertEquals(1, regolaSede.getIdRiferimento());
        assertEquals("VAL1", regolaSede.getValore());
        assertNotNull(regolaSede.getDataInizio());
        assertNotNull(regolaSede.getDataFine());
    }

    @Test
    public void testSetter() {
        regolaSede.setNome("Nuovo Nome");
        regolaSede.setValore("NEW");
        regolaSede.setDescrizione("Nuova desc");

        assertEquals("Nuovo Nome", regolaSede.getNome());
        assertEquals("NEW", regolaSede.getValore());
        assertEquals("Nuova desc", regolaSede.getDescrizione());
    }

    @Test
    public void testToString() {
        String s = regolaSede.toString();
        assertTrue(s.contains("Blocco Sede"));
        assertTrue(s.contains("SEDE"));
    }

    @Test
    public void testIsAttivaPerSede() {
        assertTrue(regolaSede.isAttivaPer(1, 99, LocalDate.now()));
        assertFalse(regolaSede.isAttivaPer(2, 99, LocalDate.now())); // sede diversa
    }

    @Test
    public void testIsAttivaPerUtente() {
        assertTrue(regolaUtente.isAttivaPer(1, 10, LocalDate.now()));
        assertFalse(regolaUtente.isAttivaPer(1, 20, LocalDate.now())); // utente diverso
    }

    @Test
    public void testIsAttivaPerGlobale() {
        assertTrue(regolaGlobale.isAttivaPer(99, 99, LocalDate.now()));
    }

    @Test
    public void testMostraRegole() {
        LocalDate oggi = LocalDate.now();
        String riga = "1;BloccoTest;BLOCCO_UTENTE;SEDE;1;X;" +
                oggi + ";" + oggi.plusDays(1) + ";desc\n";
        try {
            Files.write(Paths.get(FILE_TEST), riga.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            fail("Errore scrittura test file");
        }

        List<Regola> regole = Regola.mostraRegole(1, 99, oggi);
        assertEquals(1, regole.size());
        assertEquals("BloccoTest", regole.get(0).getNome());
    }

    @Test
    public void testMostraTutteLeRegole() {
        LocalDate oggi = LocalDate.now();
        String riga = "2;Globale;LIMITE_ORE_GIORNALIERO;GLOBALE;*;10;" +
                oggi + ";" + oggi.plusDays(2) + ";desc2\n";
        try {
            Files.write(Paths.get(FILE_TEST), riga.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            fail("Errore scrittura test file");
        }

        List<Regola> tutte = Regola.mostraTutteLeRegole();
        assertTrue(tutte.stream().anyMatch(r -> r.getNome().equals("Globale")));
    }

    @Test
    public void testValidaCoerenzaNessunConflitto() {
        // File vuoto, quindi nessun conflitto
        assertTrue(regolaSede.validaCoerenzaRegola());
    }

    @Test
    public void testValidaCoerenzaConflittoDate() {
        LocalDate oggi = LocalDate.now();
        String riga = "3;BloccoConflitto;BLOCCO_UTENTE;SEDE;1;X;" +
                oggi + ";" + oggi.plusDays(5) + ";desc\n";
        try {
            Files.write(Paths.get(FILE_TEST), riga.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            fail("Errore scrittura test file");
        }

        // Stessa sede, stesso tipo, date sovrapposte → conflitto
        assertFalse(regolaSede.validaCoerenzaRegola());
    }

    @Test
    public void testRegistraNuovaRegola() {
        assertTrue(regolaSede.registraRegola());
        List<Regola> tutte = Regola.mostraTutteLeRegole();
        assertTrue(tutte.stream().anyMatch(r -> r.getNome().equals("Blocco Sede")));
    }

    @Test
    public void testRegistraAggiornamento() {
        assertTrue(regolaSede.registraRegola());
        regolaSede.setDescrizione("Aggiornata");
        assertTrue(regolaSede.registraRegola());

        List<Regola> tutte = Regola.mostraTutteLeRegole();
        assertTrue(tutte.stream().anyMatch(r -> "Aggiornata".equals(r.getDescrizione())));
    }

    @Test
    public void testEliminaRegola() {
        assertTrue(regolaSede.registraRegola());
        assertTrue(regolaSede.eliminaRegola());

        List<Regola> tutte = Regola.mostraTutteLeRegole();
        assertTrue(tutte.isEmpty());
    }

    @Test
    public void testInizializzaNextIdDaFile() {
        assertTrue(regolaSede.registraRegola());
        Regola.inizializzaNextIdDaFile();
        // Inserisco un’altra regola senza ID
        Regola nuova = new Regola(-1, "Nuova", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.SEDE, 1, "VAL", LocalDate.now(),
                LocalDate.now().plusDays(1), "desc");
        assertTrue(nuova.registraRegola());
        assertTrue(nuova.getId() > regolaSede.getId());
    }

    /** BLOCCO_UTENTE: verifica che prenotazione bloccata generi eccezione */
    @Test
    public void testBloccoUtente() throws IOException {
        // Regola BLOCCO_UTENTE per questo utente
        Regola regola = new Regola(-1, "Blocco Utente", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), "X",
                LocalDate.now(), LocalDate.now().plusDays(1), "Test blocco utente");
        assertTrue(regola.registraRegola());

        // Prenotazione dell'utente
        Prenotazione p = new Prenotazione(utente);
        p.setPostazione(postazione);
        p.setSede(postazione.getArea().getSede());
        p.setData(LocalDate.now());
        p.setFasciaOraria("09:00-11:00");
        p.registraPrenotazione();

        // Deve lanciare eccezione perché la regola blocca l'utente
        assertThrows(IllegalStateException.class, () -> Regola.verificaPrenotabilità(p));
    }

    /** BLOCCO_GIORNI: verifica che prenotazione in giorno bloccato venga rifiutata */
    @Test
    public void testBloccoGiorni() throws IOException {
        LocalDate oggi = LocalDate.now();
        String giornoSettimana = oggi.getDayOfWeek().name(); // es. "MONDAY"

        Regola regola = new Regola(-1, "Blocco Giorno", Regola.TipoRegola.BLOCCO_GIORNI,
                Regola.ScopeRegola.GLOBALE, -1, giornoSettimana,
                oggi.minusDays(1), oggi.plusDays(1), "Test blocco giorni");
        assertTrue(regola.registraRegola());

        Prenotazione p = new Prenotazione(utente);
        p.setPostazione(postazione);
        p.setSede(postazione.getArea().getSede());
        p.setData(oggi);
        p.setFasciaOraria("09:00-11:00");
        p.registraPrenotazione();

        assertThrows(IllegalStateException.class, () -> Regola.verificaPrenotabilità(p));
    }

    /** LIMITE_ORE_GIORNALIERO: verifica che superare limite ore giornaliere generi eccezione */
    @Test
    public void testLimiteOreGiornaliere() throws IOException {
        // Regola max 2 ore al giorno
        Regola regola = new Regola(-1, "Limite Ore", Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), "2",
                LocalDate.now(), LocalDate.now().plusDays(1), "Max 2 ore");
        assertTrue(regola.registraRegola());

        // Prima prenotazione 09:00-11:00 (2 ore)
        Prenotazione p1 = new Prenotazione(utente);
        p1.setPostazione(postazione);
        p1.setSede(postazione.getArea().getSede());
        p1.setData(LocalDate.now());
        p1.setFasciaOraria("09:00-11:00");
        p1.registraPrenotazione();

        // Seconda prenotazione 11:00-12:00 (1 ora, supera limite)
        Prenotazione p2 = new Prenotazione(utente);
        p2.setPostazione(postazione);
        p2.setSede(postazione.getArea().getSede());
        p2.setData(LocalDate.now());
        p2.setFasciaOraria("11:00-12:00");
        p2.registraPrenotazione();

        assertThrows(IllegalStateException.class, () -> Regola.verificaPrenotabilità(p2));
    }

    /** Regola non attiva fuori date: prenotazione deve passare */
    @Test
    public void testRegolaNonAttiva() throws IOException {
        LocalDate ieri = LocalDate.now().minusDays(2);
        LocalDate ieriFine = ieri.plusDays(1);

        Regola regola = new Regola(-1, "Blocco Vecchio", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), "X",
                ieri, ieriFine, "Regola scaduta");
        assertTrue(regola.registraRegola());

        Prenotazione p = new Prenotazione(utente);
        p.setPostazione(postazione);
        p.setSede(postazione.getArea().getSede());
        p.setData(LocalDate.now());
        p.setFasciaOraria("09:00-11:00");
        p.registraPrenotazione();

        // Deve passare perché la regola non è attiva oggi
        assertDoesNotThrow(() -> Regola.verificaPrenotabilità(p));
    }

}

