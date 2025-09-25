package com.leone.app.domain;

import org.junit.jupiter.api.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestPrenotazione {

    private static final String FILE_TEST = "PrenotazioniTest.txt";
    private static final String FILE_TEST_CheckIn = "CheckInTest.txt";

    private Utente utente;
    private Sede sede;
    private Area area;
    private Dotazione dotazione;
    private Postazione postazione;
    private Prenotazione prenotazione;

    @BeforeEach
    public void setUp() throws IOException {
        // Imposta file test
        Prenotazione.setFileName(FILE_TEST);
        CheckIn.setFileName(FILE_TEST_CheckIn);
        try (FileWriter fw = new FileWriter(FILE_TEST)) {
            fw.write("// id,postazione,data,fascia,idUtente,uuid\n");
        }

        // Crea il file di CheckInTest se non esiste
        File fCheck = new File(FILE_TEST_CheckIn);
        if (!fCheck.exists()) {
            fCheck.createNewFile();
        }

        utente = new Utente(1, "Mario", "Rossi", "mario@test.com", "USER");
        sede = new Sede(1, "Sede Centrale", "Via Roma");
        area = new Area(1, "OpenSpace", "Area principale di lavoro", sede);
        dotazione = new Dotazione(1, "PC", "Portatile");
        postazione = new Postazione(1, "A1", "Piano Terra", true, area, dotazione);

        prenotazione = new Prenotazione(utente);
        prenotazione.setPostazione(postazione);
        prenotazione.setData(LocalDate.now());
        prenotazione.setFasciaOraria("09:00-11:00");
    }

    @AfterEach
    public void tearDown() {
        // Ripristina file di default
        Prenotazione.setFileName("Prenotazioni.txt");
        CheckIn.setFileName("CheckIn.txt");

        // Pulisce i riferimenti agli oggetti
        utente = null;
        area = null;
        dotazione = null;
        postazione = null;
        prenotazione = null;

        // Elimina i file di test se esistono
        File f1 = new File(FILE_TEST_CheckIn);
        if (f1.exists()) {
            f1.delete();
        }
        File f2 = new File(FILE_TEST_CheckIn);
        if (f2.exists()) {
            f2.delete();
        }
    }

    @Test
    public void testGetterSetter() {
        prenotazione.setSede(area.getSede());
        prenotazione.setVicinoFinestra(true);
        prenotazione.setIdArea(area.getIdArea());
        prenotazione.setIdDotazione(dotazione.getIdDotazione());

        assertEquals("09:00-11:00", prenotazione.getFasciaOraria());
        assertEquals(area.getSede(), prenotazione.getSede());
        assertTrue(prenotazione.isVicinoFinestra());
        assertEquals(area.getIdArea(), prenotazione.getIdArea());
        assertEquals(dotazione.getIdDotazione(), prenotazione.getIdDotazione());
    }

    @Test
    public void testRegoleGestione() {
        Regola r1 = new Regola(1, "Blocca", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.UTENTE, 1, "", LocalDate.now(), LocalDate.now().plusDays(1), "desc");

        prenotazione.aggiungiRegola(r1);
        assertEquals(1, prenotazione.getRegoleApplicate().size());

        prenotazione.rimuoviRegola(r1);
        assertEquals(0, prenotazione.getRegoleApplicate().size());

        prenotazione.aggiungiRegola(r1);
        prenotazione.resetRegole();
        assertTrue(prenotazione.getRegoleApplicate().isEmpty());
    }

    @Test
    public void testDurataEFormattazione() {
        assertEquals(2.0, prenotazione.getDurataOre());
        assertEquals("2:00", prenotazione.formatOreEMinuti(120));
    }

    @Test
    public void testDisponibilita() {
        assertTrue(prenotazione.verificaDisponibilità(postazione.getIdPostazione(),
                LocalDate.now(), "09:00-11:00"));
    }

    @Test
    public void testRegistraEMostraPrenotazioni() {
        String pathQr = prenotazione.registraPrenotazione();
        assertNotNull(pathQr);
        assertTrue(new File(FILE_TEST).exists());

        List<Prenotazione> tutte = Prenotazione.mostraTutteLePrenotazioni();
        assertEquals(1, tutte.size());
        assertEquals(utente.getIdUtente(), tutte.get(0).getUtente().getIdUtente());
    }

    @Test
    public void testAnnullaPrenotazione() {
        prenotazione.registraPrenotazione();
        assertTrue(prenotazione.annullaPrenotazione());

        List<Prenotazione> tutte = Prenotazione.mostraTutteLePrenotazioni();
        assertEquals(0, tutte.size());
    }

    @Test
    public void testValidaPrenotazione() {
        LocalDate oggi = LocalDate.now();
        LocalTime ora = LocalTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        // 1) Troppo presto → inizio tra 5 minuti
        String fasciaTroppoPresto = ora.plusMinutes(5).format(fmt) + "-" + ora.plusHours(2).format(fmt);
        Prenotazione pTroppoPresto = new Prenotazione(1, oggi, fasciaTroppoPresto, utente, postazione);
        assertEquals(Prenotazione.StatoPrenotazione.TROPPO_PRESTO,
                Prenotazione.validaPrenotazione(pTroppoPresto));

        // 2) Valida → inizio 5 minuti fa
        String fasciaValida = ora.minusMinutes(5).format(fmt) + "-" + ora.plusHours(2).format(fmt);
        Prenotazione pValida = new Prenotazione(2, oggi, fasciaValida, utente, postazione);
        assertEquals(Prenotazione.StatoPrenotazione.VALIDA,
                Prenotazione.validaPrenotazione(pValida));

        // 3) Scaduta → inizio 20 minuti fa (scaduta dopo 15 minuti)
        String fasciaScaduta = ora.minusMinutes(20).format(fmt) + "-" + ora.plusHours(1).format(fmt);
        Prenotazione pScaduta = new Prenotazione(3, oggi, fasciaScaduta, utente, postazione);
        assertEquals(Prenotazione.StatoPrenotazione.SCADUTA,
                Prenotazione.validaPrenotazione(pScaduta));

        // Caso errore → fascia malformata
        // Silenzia System.err
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        try {
            // Fascia malformata
            Prenotazione pErrore = new Prenotazione(3, LocalDate.now(), "XX", utente, postazione);
            assertEquals(Prenotazione.StatoPrenotazione.ERRORE,
                    Prenotazione.validaPrenotazione(pErrore));
        } finally {
            // Ripristina System.err
            System.setErr(originalErr);
        }
    }


    @Test
    public void testConflittoConRegolaBloccoUtente() {
        Regola bloccoUtente = new Regola(1, "Blocca utente", Regola.TipoRegola.BLOCCO_UTENTE,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), "", LocalDate.now(),
                LocalDate.now().plusDays(1), "Blocco test");

        assertTrue(prenotazione.verificaConflittoPrenotazione(bloccoUtente));
    }

    @Test
    public void testTrovaPrenotazioneByQrCode() throws IOException {
        // Fascia oraria dinamica: inizio 5 minuti fa, fine 2 ore dopo
        LocalTime now = LocalTime.now();
        LocalTime inizio = now.minusMinutes(5);
        LocalTime fine = now.plusHours(2);
        String fasciaOraria = inizio.format(DateTimeFormatter.ofPattern("HH:mm")) + "-"
                + fine.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Scrivo QR fittizio nel file
        String qrFittizio = "qrTest123";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_TEST, true))) {
            bw.write(String.format("1,%d,%s,%s,%d,%s\n",
                    postazione.getIdPostazione(),
                    LocalDate.now(),
                    fasciaOraria,
                    utente.getIdUtente(),
                    qrFittizio));
        }

        Prenotazione trovata = Prenotazione.trovaPrenotazione(qrFittizio);
        assertNotNull(trovata);
        assertEquals(utente.getIdUtente(), trovata.getUtente().getIdUtente());
    }

    @Test
    public void testTrovaPrenotazioneScadutaByQrCode() throws IOException {
        // Fascia dinamica: inizio 20 minuti fa, fine 1 ora dopo → oltre i 15 minuti
        LocalTime now = LocalTime.now();
        LocalTime inizio = now.minusMinutes(20);
        LocalTime fine = now.plusHours(1);
        String fasciaOraria = inizio.format(DateTimeFormatter.ofPattern("HH:mm")) + "-"
                + fine.format(DateTimeFormatter.ofPattern("HH:mm"));

        String qrFittizio = "qrScaduto123";

        // Scrivo QR fittizio nel file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_TEST, true))) {
            bw.write(String.format("2,%d,%s,%s,%d,%s\n",
                    postazione.getIdPostazione(),
                    LocalDate.now(),
                    fasciaOraria,
                    utente.getIdUtente(),
                    qrFittizio));
        }

        // Deve lanciare eccezione perché la prenotazione è scaduta
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            Prenotazione.trovaPrenotazione(qrFittizio);
        });

        assertTrue(exception.getMessage().contains("Prenotazione scaduta"));
    }

    @Test
    public void testCheckInGiaEffettuato() throws IOException {
        // Scrivo una prenotazione nel file delle prenotazioni
        LocalTime inizio = LocalTime.now().minusMinutes(5);
        LocalTime fine = LocalTime.now().plusHours(1);
        String fascia = inizio.format(DateTimeFormatter.ofPattern("HH:mm")) + "-" +
                fine.format(DateTimeFormatter.ofPattern("HH:mm"));
        String qrCode = "qrCheckIn";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_TEST, true))) {
            bw.write(String.format("10,%d,%s,%s,%d,%s\n",
                    postazione.getIdPostazione(),
                    LocalDate.now(),
                    fascia,
                    utente.getIdUtente(),
                    qrCode));
        }

        // Recupero la prenotazione registrata
        Prenotazione pren = Prenotazione.trovaPrenotazione(qrCode);

        // Registra il check-in sul file di test
        CheckIn checkIn = new CheckIn(pren.getUtente().getIdUtente(), pren.getId(), pren.getPostazione().getIdPostazione(), "metodoTest");
        assertTrue(checkIn.registraCheckIn());

        // Ora la stessa prenotazione deve sollevare l'eccezione
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            Prenotazione.trovaPrenotazione(qrCode);
        });

        assertTrue(exception.getMessage().contains("Check-in già effettuato"));
    }

    @Test
    public void testRegolaLimiteOreGiornaliere() {
        // Fascia lunga 5 ore
        Prenotazione p = new Prenotazione(1, LocalDate.now(), "09:00-14:00", utente, postazione);

        // Regola limite giornaliero 4 ore
        Regola r = new Regola(1, "Limite 4h", Regola.TipoRegola.LIMITE_ORE_GIORNALIERO,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), "4", LocalDate.now(), LocalDate.now(), "");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            p.applicaRegola(r);
        });

        assertTrue(exception.getMessage().contains("Superato il limite di 4 ore giornaliere"));
    }

    @Test
    public void testRegolaBloccoGiorni() {
        // Prenotazione di oggi
        LocalDate oggi = LocalDate.now();
        Prenotazione p = new Prenotazione(1, oggi, "09:00-11:00", utente, postazione);

        // Regola che blocca il giorno corrente
        String giorno = oggi.getDayOfWeek().name();
        Regola r = new Regola(1, "Blocca giorno", Regola.TipoRegola.BLOCCO_GIORNI,
                Regola.ScopeRegola.UTENTE, utente.getIdUtente(), giorno, oggi, oggi, "");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            p.applicaRegola(r);
        });

        assertTrue(exception.getMessage().contains("Non è possibile effettuare prenotazioni nei seguenti giorni"));
    }

    @Test
    public void testPrenotazioniSovrapposte() throws IOException {
        // Fascia iniziale 09:00-11:00
        Prenotazione p1 = new Prenotazione(1, LocalDate.now(), "09:00-11:00", utente, postazione);
        p1.registraPrenotazione();

        // Caso 1: Fascia sovrapposta 10:00-12:00: deve generare eccezione
        Prenotazione p2 = new Prenotazione(2, LocalDate.now(), "10:00-12:00", utente, postazione);
        Exception exception = assertThrows(IllegalStateException.class, () -> p2.registraPrenotazione());
        assertTrue(exception.getMessage().contains("sovrapposta"));

        // Caso 2: Fascia non sovrapposta 12:00-14:00: deve registrarsi senza eccezioni
        Prenotazione p3 = new Prenotazione(3, LocalDate.now(), "12:00-14:00", utente, postazione);
        assertDoesNotThrow(() -> p3.registraPrenotazione());
    }



}