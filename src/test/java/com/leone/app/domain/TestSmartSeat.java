package com.leone.app.domain;

import org.junit.jupiter.api.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TestSmartSeat {

    // Sostituire con l'indirizzo email su cui si desidera ricevere le notifiche durante i test
    private static final String EMAIL_TEST = "test@example.com";

    private static final String FILE_UTENTI_TEST = "UtentiTest.txt";
    private static final String FILE_DOTAZIONI_TEST = "DotazioniTest.txt";
    private static final String FILE_SEDI_TEST = "SediTest.txt";
    private static final String FILE_REGOLE_TEST = "RegoleTest.txt";
    private static final String FILE_PRENOTAZIONI_TEST = "PrenotazioniTest.txt";
    private static final String FILE_CHECKIN_TEST = "CheckInTest.txt";

    private SmartSeat smartSeat;

    @BeforeEach
    public void setUp() {
        // Pulizia dei file di test
        pulisciFile(FILE_UTENTI_TEST);
        pulisciFile(FILE_DOTAZIONI_TEST);
        pulisciFile(FILE_SEDI_TEST);
        pulisciFile(FILE_REGOLE_TEST);
        pulisciFile(FILE_PRENOTAZIONI_TEST);
        pulisciFile(FILE_CHECKIN_TEST);

        // Inserisci dati di base nei file di test
        scriviFileTest(FILE_UTENTI_TEST, "1,Mario,Rossi,mario.rossi@email.com,USER\n");
        scriviFileTest(FILE_DOTAZIONI_TEST, "1,PC,Laptop per ufficio\n");
        scriviFileTest(FILE_SEDI_TEST, "1,Sede Centrale,Via Roma 1, Roma\n");

        // Forza SmartSeat a usare i file di test
        Utente.setFileName(FILE_UTENTI_TEST);
        Dotazione.setFileName(FILE_DOTAZIONI_TEST);
        Sede.setFileName(FILE_SEDI_TEST);
        Regola.setFileName(FILE_REGOLE_TEST);
        Prenotazione.setFileName(FILE_PRENOTAZIONI_TEST);
        CheckIn.setFileName(FILE_CHECKIN_TEST);

        // Istanza SmartSeat
        smartSeat = SmartSeat.getInstance();
    }

    @AfterEach
    public void tearDown() {
        smartSeat = null;

        // Ripristina eventuali file di produzione
        Utente.setFileName("Utenti.txt");
        Dotazione.setFileName("Dotazioni.txt");
        Sede.setFileName("Sedi.txt");
        Regola.setFileName("Regole.txt");
        Prenotazione.setFileName("Prenotazioni.txt");
    }

    private void pulisciFile(String fileName) {
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write("");
        } catch (IOException e) {
            fail("Errore nella pulizia file di test: " + e.getMessage());
        }
    }

    private void scriviFileTest(String fileName, String contenuto) {
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(contenuto);
        } catch (IOException e) {
            fail("Errore scrittura file di test: " + e.getMessage());
        }
    }

    @Test
    public void testLoadUtenti() {
        smartSeat.loadUtenti();
        assertFalse(smartSeat.getElencoUtenti().isEmpty());
    }

    @Test
    public void testLoadDotazioni() {
        smartSeat.loadDotazioni();
        assertFalse(smartSeat.getElencoDotazioni().isEmpty());
    }

    @Test
    public void testLoadSedi() {
        smartSeat.loadSedi();
        assertFalse(smartSeat.getElencoSedi().isEmpty());
    }


    @Test
    public void testSingleton() {
        SmartSeat ss1 = SmartSeat.getInstance();
        SmartSeat ss2 = SmartSeat.getInstance();
        assertSame(ss1, ss2);
    }

    @Test
    public void testAvviaPrenotazione() {
        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);

        smartSeat.avviaPrenotazione(1);
        assertNotNull(smartSeat.getPrenotazioneInCorso());
        assertEquals(u, smartSeat.getPrenotazioneInCorso().getUtente());
    }

    @Test
    public void testSelezionaParametri() {
        Sede sede = new Sede(1, "SedeTest", "Via Test");
        smartSeat.getElencoSedi().put(1, sede);

        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);
        smartSeat.avviaPrenotazione(1);

        LocalDate data = LocalDate.now().plusDays(1);
        smartSeat.selezionaParametri(1, data, "09:00-10:00");

        assertEquals(sede, smartSeat.getPrenotazioneInCorso().getSede());
        assertEquals(data, smartSeat.getPrenotazioneInCorso().getData());
        assertEquals("09:00-10:00", smartSeat.getPrenotazioneInCorso().getFasciaOraria());
    }

    @Test
    public void testSelezionaFiltri() {
        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);
        smartSeat.avviaPrenotazione(1);

        smartSeat.selezionaFiltri(true, 2, 3);
        assertTrue(smartSeat.getPrenotazioneInCorso().isVicinoFinestra());
        assertEquals(2, smartSeat.getPrenotazioneInCorso().getIdDotazione());
        assertEquals(3, smartSeat.getPrenotazioneInCorso().getIdArea());
    }

    @Test
    public void testMostraPostazioniDisponibili() {
        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);
        smartSeat.avviaPrenotazione(1);

        Sede sede = new Sede(1, "SedeTest", "Via Test");
        Area area = new Area(1, "AreaTest", "Desc", sede);
        Postazione p = new Postazione(1, "P1", "Pos1", true, area, new Dotazione(1, "PC", "Desc"));
        area.addPostazione(p);
        sede.addArea(area);
        smartSeat.getElencoSedi().put(1, sede);

        smartSeat.selezionaParametri(1, LocalDate.now().plusDays(1), "09:00-10:00");
        smartSeat.selezionaFiltri(true, 1, 1);

        List<Postazione> disponibili = smartSeat.mostraPostazioniDisponibili();
        assertTrue(disponibili.contains(p));
    }

    @Test
    public void testSelezionaPostazione() {
        // Setup
        Sede sede = new Sede(1, "SedeTest", "Via Test");
        Area area = new Area(1, "AreaTest", "Desc", sede);
        Dotazione dot = new Dotazione(1, "PC", "Desc");
        Postazione postazione = new Postazione(1, "P1", "Pos1", true, area, dot);

        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);

        // Genera la prenotazione in corso usando il metodo esistente
        smartSeat.avviaPrenotazione(u.getIdUtente());

        // Ora la prenotazioneInCorso è inizializzata, possiamo settare data e fascia
        Prenotazione pren = smartSeat.getPrenotazioneInCorso();
        pren.setData(LocalDate.now().plusDays(1));
        pren.setFasciaOraria("09:00-10:00");

        // Esegui metodo
        boolean result = smartSeat.selezionaPostazione(postazione);

        assertTrue(result);
        assertEquals(postazione, smartSeat.getPrenotazioneInCorso().getPostazione());
    }

    @Test
    public void testSelezionaPostazioneFalliscePerRegola() {
        // Setup
        Utente u = new Utente(1, "Mario", "Rossi", "mario@example.com", "USER");
        smartSeat.getElencoUtenti().put(u.getIdUtente(), u);
        smartSeat.avviaPrenotazione(u.getIdUtente());

        // Crea sede, area, postazione
        Sede sede = new Sede(1, "SedeTest", "Via Test");
        Area area = new Area(1, "AreaTest", "Desc", sede);
        Postazione postazione = new Postazione(1, "P1", "Pos1", true, area, new Dotazione(1, "PC", "Desc"));
        area.addPostazione(postazione);
        sede.addArea(area);
        smartSeat.getElencoSedi().put(1, sede);

        // Imposta parametri prenotazione
        smartSeat.selezionaParametri(1, LocalDate.now().plusDays(1), "09:00-10:00");
        smartSeat.selezionaFiltri(true, 1, 1);

        // Regola che blocca l'utente
        LocalDate oggi = LocalDate.now();
        Regola blocco = new Regola(-1, "Blocco test", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.UTENTE, u.getIdUtente(), "", oggi, oggi.plusDays(5), "Blocco per test");
        smartSeat.confermaRegola(blocco);

        // Esegui metodo
        boolean result = smartSeat.selezionaPostazione(postazione);

        // La prenotazione non deve andare a buon fine
        assertFalse(result);
    }


    @Test
    public void testConfermaRegola() {
        // Setup
        LocalDate oggi = LocalDate.now();
        Regola reg = new Regola(-1, "TestRegola", Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.GLOBALE, -1, "5", oggi, oggi.plusDays(10), "Descrizione");

        // Esegui metodo
        boolean result = smartSeat.confermaRegola(reg);
        assertTrue(result); // dovrebbe registrare correttamente
    }

    @Test
    public void testAnnullaRegola() {
        // Setup
        LocalDate oggi = LocalDate.now();
        Regola reg = new Regola(-1, "TestRegola", Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.GLOBALE, -1, "5", oggi, oggi.plusDays(10), "Descrizione");

        // Registriamo la regola prima di cancellare
        smartSeat.confermaRegola(reg);

        // Simula input "s" per confermare l'eliminazione
        String inputSimulato = "s\n";
        InputStream originale = System.in;
        try {
            System.setIn(new ByteArrayInputStream(inputSimulato.getBytes()));

            // Esegui metodo
            boolean result = smartSeat.annullaRegola(reg);

            assertTrue(result); // regola eliminata con successo
        } finally {
            // Ripristina System.in
            System.setIn(originale);
        }
    }


    @Test
    public void testAvviaConfigurazioneRegole() {
        // Esegui metodo
        List<Regola> regole = smartSeat.avviaConfigurazioneRegole();

        // Asserts
        assertNotNull(regole);
        // Se ci sono regole di default, verifica almeno qualche proprietà
        if (!regole.isEmpty()) {
            Regola r = regole.get(0);
            assertNotNull(r.getNome());
            assertNotNull(r.getTipo());
        }
    }

    @Test
    void testConfermaPrenotazione() {
        // Utente di test
        Utente utente = new Utente(1, "Mario", "Rossi", EMAIL_TEST, "utente");

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

        // Prenotazione completa
        Prenotazione prenotazione = new Prenotazione(
                -1,
                LocalDate.now().plusDays(1),   // data futura
                "09:00-11:00",
                utente,
                postazione
        );
        prenotazione.setSede(sede);

        // Imposta la prenotazione in corso nello SmartSeat
        smartSeat.setPrenotazioneInCorso(prenotazione);

        // Verifica che confermare la prenotazione non lanci eccezioni
        assertDoesNotThrow(() -> smartSeat.confermaPrenotazione());
    }


    @Test
    void testEffettuaCheckIn() throws Exception {
        // Utente di test
        Utente utente = new Utente(1, "Mario", "Rossi", EMAIL_TEST, "utente");

        // Sede e area di test
        Sede sede = new Sede(1, "Sede Centrale", "Via Roma 1");
        Area area = new Area(1, "Open Space", "Area comune", sede);
        Dotazione dotazione = new Dotazione(1, "Monitor", "Monitor 27 pollici");
        Postazione postazione = new Postazione(1, "P01", "Primo piano", true, area, dotazione);

        // Fascia oraria dinamica: inizio 1 minuto fa, durata 2 ore
        LocalTime inizio = LocalTime.now().minusMinutes(1);
        LocalTime fine = inizio.plusHours(2);
        String fasciaOraria = inizio.format(DateTimeFormatter.ofPattern("HH:mm")) + "-" +
                fine.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Prenotazione valida
        Prenotazione prenotazione = new Prenotazione(1, LocalDate.now(), fasciaOraria, utente, postazione);
        prenotazione.setSede(sede);

        // Registra prenotazione e ottieni il QR path
        String qrPath = prenotazione.registraPrenotazione();

        // Test check-in usando il file generato dalla prenotazione
        File qrFile = new File(qrPath);
        assertTrue(smartSeat.effettuaCheckInDaFile(qrFile));
    }



}

