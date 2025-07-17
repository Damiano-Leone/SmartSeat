package com.leone.app.domain;

import java.io.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class Prenotazione {
    private static int nextId = 1;
    private final int id;
    private final Utente utente;
    private Sede sede;
    private Postazione postazione;
    private LocalDate data;
    private String fasciaOraria;
    private boolean vicinoFinestra;
    private int idDotazione;
    private int idArea;
    private final List<Regola> regoleApplicate = new ArrayList<>();

    public Prenotazione(Utente utente) {
        Prenotazione.inizializzaNextIdDaFile();
        this.id = nextId++;
        this.utente = utente;
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
        try (BufferedReader reader = new BufferedReader(new FileReader("Prenotazioni.txt"))) {
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

        try (BufferedReader reader = new BufferedReader(new FileReader("Prenotazioni.txt"))) {
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
                    boolean overlap = !(userEnd.isBefore(bookedStart) || bookedEnd.isBefore(userStart));
                    if (overlap) return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel controllo disponibilità: " + e.getMessage());
        }

        return true;
    }

    public void applicaRegola(Regola r) {
        switch (r.getTipo()) {
            case BLOCCO_UTENTE -> {
                String msg = "⛔ Prenotazione bloccata per l'utente: " + r.getDescrizione();
                if (r.getDataFine() != null) {
                    msg += " (blocco valido fino al " + r.getDataFine().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                }
                throw new IllegalStateException(msg);
            }
            case LIMITE_ORE_GIORNALIERO -> {
                int limite = Integer.parseInt(r.getValore());
                int orePrenotazioneCorrente = getDurataOre();

                if (orePrenotazioneCorrente > limite) {
                    throw new IllegalStateException("⛔ Superato limite di " + limite + " ore giornaliere. La tua prenotazione dura " + orePrenotazioneCorrente + " ore.");
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

    public int getDurataOre() {
        if (fasciaOraria == null || !fasciaOraria.contains("-")) return 0;

        try {
            String[] parts = fasciaOraria.split("-");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            return (int) Duration.between(start, end).toHours();
        } catch (Exception e) {
            return 0;
        }
    }

    public static void registraPrenotazione(Prenotazione p) {
        String riga = String.format("%d,%d,%s,%s,%d",
                p.getId(),
                p.getPostazione().getIdPostazione(),
                p.getData(),
                p.getFasciaOraria(),
                p.getUtente().getIdUtente()
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Prenotazioni.txt", true))) {
            writer.newLine();
            writer.write(riga);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
