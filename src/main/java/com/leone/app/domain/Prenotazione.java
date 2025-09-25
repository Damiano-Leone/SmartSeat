package com.leone.app.domain;

import com.leone.app.util.QRCode;

import java.io.*;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class Prenotazione {
    private static int nextId = 1;
    private int id;
    private final Utente utente;
    private Sede sede;
    private Postazione postazione;
    private LocalDate data;
    private String fasciaOraria;
    private boolean vicinoFinestra;
    private int idDotazione;
    private int idArea;
    private final List<Regola> regoleApplicate = new ArrayList<>();
    private static final Object FILE_LOCK = new Object();

    private static String FILE_NAME = "Prenotazioni.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Prenotazione(Utente utente) {
        this.id = -1;
        this.utente = utente;
    }

    public Prenotazione(int id, LocalDate data, String fasciaOraria, Utente utente, Postazione postazione) {
        this.id = id;
        this.data = data;
        this.fasciaOraria = fasciaOraria;
        this.utente = utente;
        this.postazione = postazione;
        this.sede = null;
        this.vicinoFinestra = false;
        this.idDotazione = 0;
        this.idArea = 0;
    }

    public int getId() {
        return id;
    }

    public Utente getUtente() {
        return utente;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public Postazione getPostazione() {
        return postazione;
    }

    public void setPostazione(Postazione postazione) {
        this.postazione = postazione;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getFasciaOraria() {
        return fasciaOraria;
    }

    public void setFasciaOraria(String fasciaOraria) {
        this.fasciaOraria = fasciaOraria;
    }

    public boolean isVicinoFinestra() {
        return vicinoFinestra;
    }

    public void setVicinoFinestra(boolean vicinoFinestra) {
        this.vicinoFinestra = vicinoFinestra;
    }

    public int getIdDotazione() {
        return idDotazione;
    }

    public void setIdDotazione(int idDotazione) {
        this.idDotazione = idDotazione;
    }

    public int getIdArea() {
        return idArea;
    }

    public void setIdArea(int idArea) {
        this.idArea = idArea;
    }

    public List<Regola> getRegoleApplicate() {
        return regoleApplicate;
    }

    public void aggiungiRegola(Regola regola) {
        this.regoleApplicate.add(regola);
    }

    public void rimuoviRegola(Regola regola) {
        this.regoleApplicate.remove(regola);
    }

    public void resetRegole() {
        this.regoleApplicate.clear();
    }

    public String riepilogoPrenotazione() {
        return "Prenotazione{" +
                "utente=" + utente +
                ", sede=" + sede +
                ", postazione=" + postazione +
                ", data=" + data +
                ", fasciaOraria='" + fasciaOraria + '\'' +
                ", vicinoFinestra=" + vicinoFinestra +
                ", idDotazione=" + idDotazione +
                ", idArea=" + idArea +
                ", regoleApplicate=" + regoleApplicate +
                '}';
    }

    @Override
    public String toString() {
        return "Prenotazione{" +
                "id=" + id +
                ", utente=" + utente +
                ", sede=" + sede +
                ", postazione=" + postazione +
                ", data=" + data +
                ", fasciaOraria='" + fasciaOraria + '\'' +
                ", vicinoFinestra=" + vicinoFinestra +
                ", idDotazione=" + idDotazione +
                ", idArea=" + idArea +
                ", regoleApplicate=" + regoleApplicate +
                '}';
    }

    public static void inizializzaNextIdDaFile() {
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                int maxId = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("//")) continue;
                    String[] campi = line.split(",");
                    int id = Integer.parseInt(campi[0]);
                    if (id > maxId) {
                        maxId = id;
                    }
                }
                nextId = maxId + 1;
            } catch (IOException | NumberFormatException e) {
                nextId = 1; // fallback sicuro
            }
        }
    }

    public boolean verificaParametri(Postazione p) {
        // Verifica disponibilità
        if (!verificaDisponibilità(p.getIdPostazione(), data, fasciaOraria)) return false;

        // Filtro finestra
        if (vicinoFinestra && !p.isVicinanzaAFinestra()) return false;

        // Filtro dotazione
        if (idDotazione != -1 && p.getDotazione().getIdDotazione() != idDotazione) return false;

        // Filtro area
        if (idArea != -1 && p.getArea().getIdArea() != idArea) return false;

        return true;
    }

    public boolean verificaDisponibilità(int idPostazione, LocalDate data, String fascia) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Ignora righe vuote o commenti
                    if (line.isEmpty() || line.startsWith("//")) continue;

                    String[] parts = line.split(",");
                    if (parts.length < 5) continue;

                    int idPrenotazione = Integer.parseInt(parts[0].trim());
                    int id = Integer.parseInt(parts[1].trim());
                    LocalDate dataPrenotata = LocalDate.parse(parts[2].trim(), dateFormatter);
                    String fasciaPrenotata = parts[3].trim();
                    int idUtente = Integer.parseInt(parts[4].trim());

                    if (id == idPostazione && data.equals(dataPrenotata)) {
                        // Parsing fascia oraria dell’utente
                        String[] userFasciaParts = fascia.split("-");
                        LocalTime userStart = LocalTime.parse(userFasciaParts[0], timeFormatter);
                        LocalTime userEnd = LocalTime.parse(userFasciaParts[1], timeFormatter);

                        // Parsing fascia oraria della prenotazione esistente
                        String[] bookedFasciaParts = fasciaPrenotata.split("-");
                        LocalTime bookedStart = LocalTime.parse(bookedFasciaParts[0], timeFormatter);
                        LocalTime bookedEnd = LocalTime.parse(bookedFasciaParts[1], timeFormatter);

                        // Controllo sovrapposizione
                        boolean overlap = userStart.isBefore(bookedEnd) && userEnd.isAfter(bookedStart);
                        if (overlap) return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore nel controllo disponibilità: " + e.getMessage());
            }
        }
        return true;
    }

    public void applicaRegola(Regola r) {
        switch (r.getTipo()) {
            case BLOCCO_UTENTE -> {
                String msg = "⛔ Prenotazione bloccata per l'utente: " + r.getDescrizione();
                if (r.getDataInizio() != null && r.getDataFine() != null) {
                    msg += " (blocco valido dal "
                            + r.getDataInizio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + " al "
                            + r.getDataFine().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            + ")";
                }
                throw new IllegalStateException(msg);
            }
            case LIMITE_ORE_GIORNALIERO -> {
                int limite = Integer.parseInt(r.getValore());
                long minutiTotali = (long)(calcolaOreGiornaliereUtente(this.data) * 60);
                minutiTotali += (long)(getDurataOre() * 60); // minuti totali
                if (minutiTotali > limite * 60) {
                    String durata = formatOreEMinuti(minutiTotali);
                    throw new IllegalStateException("⛔ Superato il limite di " + limite +
                            " ore giornaliere. Le tue prenotazioni per il " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                            " hanno una durata complessiva di " + durata + " ore.");
                }
            }
            case BLOCCO_GIORNI -> {
                Set<DayOfWeek> giorniBloccati = Arrays.stream(r.getValore().split(","))
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(DayOfWeek::valueOf)
                        .collect(Collectors.toSet());

                DayOfWeek giornoPrenotazione = getData().getDayOfWeek();

                if (giorniBloccati.contains(giornoPrenotazione)) {
                    String giorniBloccatiStr = giorniBloccati.stream()
                            .map(d -> d.getDisplayName(TextStyle.FULL, Locale.ITALIAN).toUpperCase())
                            .collect(Collectors.joining(", "));

                    String giornoPrenotatoStr = giornoPrenotazione.getDisplayName(TextStyle.FULL, Locale.ITALIAN).toUpperCase();

                    throw new IllegalStateException(
                            "⛔ Non è possibile effettuare prenotazioni nei seguenti giorni: " + giorniBloccatiStr +
                                    ". La prenotazione selezionata ricade di " + giornoPrenotatoStr + "."
                    );
                }
            }

        }
    }

    public String formatOreEMinuti(long minutiTotali) {
        long ore = minutiTotali / 60;
        long minuti = minutiTotali % 60;
        return String.format("%d:%02d", ore, minuti);
    }

    public double calcolaOreGiornaliereUtente(LocalDate data) {
        double oreTotali = 0;
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("//")) continue;

                    String[] campi = line.split(",");
                    int idUtenteFile = Integer.parseInt(campi[4]);
                    LocalDate dataFile = LocalDate.parse(campi[2]);
                    String fasciaFile = campi[3];

                    if (idUtenteFile == this.utente.getIdUtente() && dataFile.equals(data)) {
                        // calcola durata in ore decimali della prenotazione esistente
                        String[] parts = fasciaFile.split("-");
                        LocalTime start = LocalTime.parse(parts[0]);
                        LocalTime end = LocalTime.parse(parts[1]);
                        long minuti = Duration.between(start, end).toMinutes();
                        oreTotali += minuti / 60.0;
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return oreTotali;
    }


    public double getDurataOre() {
        if (fasciaOraria == null || !fasciaOraria.contains("-")) return 0;

        try {
            String[] parts = fasciaOraria.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);

            long minuti = Duration.between(start, end).toMinutes();
            return minuti / 60.0; // ore decimali, es. 8.02
        } catch (Exception e) {
            return 0;
        }
    }

    public String registraPrenotazione() {
        if (utenteHaPrenotazioniSovrapposte(utente.getIdUtente(), data, fasciaOraria)) {
            throw new IllegalStateException("⛔ L'utente ha già una prenotazione sovrapposta nella stessa giornata ("
                    + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
        inizializzaNextIdDaFile();
        this.id = nextId++;
        UUID uuid = UUID.randomUUID();

        String riga = String.format("%d,%d,%s,%s,%d,%s",
                id,
                postazione.getIdPostazione(),
                data,
                fasciaOraria,
                utente.getIdUtente(),
                uuid
        );

        String pathQRCode = "qrcode/" + uuid + ".png";
        QRCode.generaQRCode(uuid.toString(), pathQRCode);
        synchronized (FILE_LOCK) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
                writer.newLine();
                writer.write(riga);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return pathQRCode;
    }

    public static List<Prenotazione> mostraTutteLePrenotazioni() {
        List<Prenotazione> prenotazioni = new ArrayList<>();
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("//")) continue;

                    String[] campi = line.split(",");

                    int idPrenotazione = Integer.parseInt(campi[0]);
                    int idPostazione = Integer.parseInt(campi[1]);
                    LocalDate data = LocalDate.parse(campi[2]);
                    String fasciaOraria = campi[3];
                    int idUtente = Integer.parseInt(campi[4]);

                    // Recupero oggetti da ID
                    Postazione postazione = Postazione.getById(idPostazione);
                    Utente utente = Utente.getById(idUtente);


                    Prenotazione p = new Prenotazione(idPrenotazione, data, fasciaOraria, utente, postazione);

                    prenotazioni.add(p);
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return prenotazioni;
    }

    public boolean annullaPrenotazione() {
        try {
            synchronized (FILE_LOCK) {
                File file = new File(FILE_NAME);
                List<String> lines = Files.readAllLines(file.toPath());
                List<String> updatedLines = new ArrayList<>();

                for (String line : lines) {
                    if (!line.startsWith(String.valueOf(this.id) + ",")) {
                        updatedLines.add(line); // mantiene tutte le altre prenotazioni
                    }
                }
                Files.write(file.toPath(), updatedLines);
            }
            return true; // annullamento riuscito
        } catch (IOException e) {
            e.printStackTrace();
            return false; // annullamento fallito
        }
    }

    private boolean utenteHaPrenotazioniSovrapposte(int idUtente, LocalDate data, String fascia) {
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("//")) continue;
                    String[] campi = line.split(",");

                    int idUtenteEsistente = Integer.parseInt(campi[4]);
                    LocalDate dataEsistente = LocalDate.parse(campi[2]);
                    String fasciaEsistente = campi[3];

                    // Stesso utente e stessa data?
                    if (idUtenteEsistente == idUtente && dataEsistente.equals(data)) {
                        if (fasceSiSovrappongono(fasciaEsistente, fascia)) {
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean fasceSiSovrappongono(String f1, String f2) {
        try {
            String[] p1 = f1.split("-");
            String[] p2 = f2.split("-");
            LocalTime start1 = LocalTime.parse(p1[0]);
            LocalTime end1 = LocalTime.parse(p1[1]);
            LocalTime start2 = LocalTime.parse(p2[0]);
            LocalTime end2 = LocalTime.parse(p2[1]);

            return start1.isBefore(end2) && start2.isBefore(end1);

        } catch (Exception e) {
            return false; // se formato non valido, ignora
        }
    }

    public static Prenotazione trovaPrenotazione(String qrCode) {
        Prenotazione pren = ottieniDettagliPrenotazione(qrCode);
        if (pren == null) {
            throw new IllegalStateException("Nessuna prenotazione valida trovata per questo QR Code. " +
                    "Il codice potrebbe riferirsi a una prenotazione scaduta o inesistente. " +
                    "Se non hai ancora prenotato una postazione, effettua prima una prenotazione e riprova il check-in.");
        }
        if (CheckIn.checkInGiaEffettuato(pren.getId())) {
            throw new IllegalStateException("Check-in già effettuato per questa prenotazione.");
        }
        StatoPrenotazione stato = validaPrenotazione(pren);

        if (stato == StatoPrenotazione.TROPPO_PRESTO) {
            throw new IllegalStateException("Check-in non consentito: sei in anticipo rispetto all’orario di inizio.");
        } else if (stato == StatoPrenotazione.SCADUTA) {
            throw new IllegalStateException("Prenotazione scaduta: sono passati più di 15 minuti dall’orario di inizio.");
        } else if (stato == StatoPrenotazione.ERRORE) {
            throw new IllegalStateException("Errore nella validazione della prenotazione.");
        }
        return pren;
    }

    private static Prenotazione ottieniDettagliPrenotazione(String qrCode) {
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] campi = line.split(",");

                    if (campi.length == 6 && campi[5].equals(qrCode)) {
                        int idPren = Integer.parseInt(campi[0]);
                        int idPostazione = Integer.parseInt(campi[1]);
                        LocalDate data = LocalDate.parse(campi[2]);
                        String fascia = campi[3];
                        int idUtente = Integer.parseInt(campi[4]);

                        Prenotazione p = new Prenotazione(
                                idPren, data, fascia,
                                new Utente(idUtente),
                                new Postazione(idPostazione)
                        );

                        return p;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public enum StatoPrenotazione {
        VALIDA,
        TROPPO_PRESTO,
        SCADUTA,
        ERRORE
    }

    // Verifica validità: entro 15 minuti dall’orario di inizio previsto
    public static StatoPrenotazione validaPrenotazione(Prenotazione p) {
        try {
            String[] orari = p.getFasciaOraria().split("-");
            LocalTime inizio = LocalTime.parse(orari[0].trim());

            LocalDateTime oraPren = LocalDateTime.of(p.getData(), inizio);
            LocalDateTime oraAttuale = LocalDateTime.now();

            Duration diff = Duration.between(oraPren, oraAttuale);

            if (diff.isNegative()) {
                return StatoPrenotazione.TROPPO_PRESTO;
            } else if (diff.toMinutes() > 15) {
                return StatoPrenotazione.SCADUTA;
            } else {
                return StatoPrenotazione.VALIDA;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return StatoPrenotazione.ERRORE;
        }
    }

    public boolean verificaConflittoPrenotazione(Regola r) {
        // Controllo preliminare: la prenotazione deve cadere nel periodo della regola
        if (this.data.isBefore(r.getDataInizio()) || this.data.isAfter(r.getDataFine())) {
            return false; // fuori dal periodo, nessun conflitto
        }

        switch (r.getTipo()) {
            case LIMITE_ORE_GIORNALIERO:
                int limite = Integer.parseInt(r.getValore());

                // Controllo se la regola si applica al contesto dell’utente
                boolean conflitto = false;

                if ((r.getScope() == Regola.ScopeRegola.UTENTE && this.utente.getIdUtente() == r.getIdRiferimento()) ||
                        (r.getScope() == Regola.ScopeRegola.SEDE && this.postazione.getArea().getSede().getIdSede() == r.getIdRiferimento()) ||
                        r.getScope() == Regola.ScopeRegola.GLOBALE) {

                    long minutiTotali = (long)(calcolaOreGiornaliereUtente(this.data) * 60);
                    conflitto = minutiTotali > limite * 60;
                }

                if (conflitto) {
                    return true; // conflitto: superato limite giornaliero
                }

                break;

            case BLOCCO_UTENTE:
                if (r.getScope() == Regola.ScopeRegola.UTENTE &&
                        this.utente.getIdUtente() == r.getIdRiferimento()) {
                    return true;
                }
                break;

            case BLOCCO_GIORNI:
                DayOfWeek giornoPrenotazione = this.data.getDayOfWeek();
                String[] giorniBloccati = r.getValore().split(",");

                for (String g : giorniBloccati) {
                    if (giornoPrenotazione.name().equalsIgnoreCase(g.trim())) {

                        // Controllo scope
                        if (r.getScope() == Regola.ScopeRegola.UTENTE &&
                                this.utente.getIdUtente() == r.getIdRiferimento()) {
                            return true;
                        }

                        if (r.getScope() == Regola.ScopeRegola.SEDE &&
                                this.postazione.getArea().getSede().getIdSede() == r.getIdRiferimento()) {
                            return true;
                        }

                        if (r.getScope() == Regola.ScopeRegola.GLOBALE) {
                            return true;
                        }
                    }
                }
                break;
        }

        return false;
    }


}
