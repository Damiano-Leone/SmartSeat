package com.leone.app.domain;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

public class SmartSeat {
    private static SmartSeat smartSeat;
    private Map<Integer, Sede> elencoSedi;
    private Map<Integer, Utente> elencoUtenti;
    private Map<Integer, Dotazione> elencoDotazioni;
    private List<Prenotazione> elencoPrenotazioni;

    private Sede sedeCorrente;
    private Postazione postazioneSelezionata;
    private Prenotazione prenotazioneInCorso;

    public SmartSeat(){
        this.elencoSedi = new HashMap<>();
        this.elencoUtenti = new HashMap<>();
        this.elencoDotazioni = new HashMap<>();
        this.elencoPrenotazioni = new LinkedList<>();

        Prenotazione.inizializzaNextIdDaFile();

        // Avviamento
        loadUtenti();
        loadDotazioni();
        loadSedi();
    }

    public static SmartSeat getInstance(){
        if(smartSeat == null)
            smartSeat = new SmartSeat();

        return smartSeat;
    }

    public void loadUtenti() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Utenti.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                int id = Integer.parseInt(tokens[0]);
                String nome = tokens[1];
                String cognome = tokens[2];
                String email = tokens[3];
                String ruolo = tokens[4];

                Utente u = new Utente(id, nome, cognome, email, ruolo);
                elencoUtenti.put(id, u);
            }
        } catch (IOException e) {
            System.err.println("Errore caricamento utenti: " + e);
        }
    }

    public void loadDotazioni() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Dotazioni.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                // Regex: split solo sulle virgole fuori dalle virgolette
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (tokens.length < 3) {
                    System.err.println("⚠️ Riga malformata in Dotazioni.txt: " + line);
                    continue;
                }

                int id = Integer.parseInt(tokens[0].trim());
                String nome = tokens[1].replace("\"", "").trim();
                String descrizione = tokens[2].replace("\"", "").trim();

                Dotazione d = new Dotazione(id, nome, descrizione);
                elencoDotazioni.put(id, d);
            }
        } catch (IOException e) {
            System.err.println("Errore caricamento dotazioni: " + e);
        }
    }

    public void loadSedi() {
        try (
                BufferedReader bfSedi = new BufferedReader(new FileReader("Sedi.txt"));
                BufferedReader bfAree = new BufferedReader(new FileReader("Aree.txt"));
                BufferedReader bfPostazioni = new BufferedReader(new FileReader("Postazioni.txt"))
        ) {
            String line;

            // 1. Carica sedi
            while ((line = bfSedi.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                if (tokens.length < 3) {
                    System.err.println("⚠️ Riga malformata in Sedi.txt: " + line);
                    continue;
                }

                int id = Integer.parseInt(tokens[0].trim());
                String nome = tokens[1].replace("\"", "").trim();
                String indirizzo = tokens[2].replace("\"", "").trim();
                Sede sede = new Sede(id, nome, indirizzo);
                elencoSedi.put(id, sede);
            }

            // 2. Carica aree e aggiungile alla sede
            while ((line = bfAree.readLine()) != null) {
                String[] tokens = line.split(",");
                int idArea = Integer.parseInt(tokens[0]);
                String nome = tokens[1];
                String descrizione = tokens[2];
                int idSede = Integer.parseInt(tokens[3]);

                Sede sede = elencoSedi.get(idSede);
                Area area = new Area(idArea, nome, descrizione, sede);
                sede.addArea(area); // metodo add dell'associazione
            }

            // 3. Carica postazioni e aggiungile alle aree
            while ((line = bfPostazioni.readLine()) != null) {
                String[] tokens = line.split(",");
                int idPostazione = Integer.parseInt(tokens[0]);
                String codice = tokens[1];
                String posizione = tokens[2];
                boolean vicinoFinestra = Boolean.parseBoolean(tokens[3]);
                int idArea = Integer.parseInt(tokens[4]);
                int idDotazione = Integer.parseInt(tokens[5]);

                // Recupera area e dotazione
                Area area = null;
                for (Sede sede : elencoSedi.values()) {
                    area = sede.getElencoAree().get(idArea);
                    if (area != null) break;
                }

                Dotazione dotazione = elencoDotazioni.get(idDotazione);

                if (area != null && dotazione != null) {
                    Postazione postazione = new Postazione(idPostazione, codice, posizione, vicinoFinestra, area, dotazione);
                    area.addPostazione(postazione);
                }
            }

        } catch (IOException e) {
            System.err.println("Errore caricamento sedi: " + e);
        }
    }

    public int selezionaUtenteDaConsole() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Seleziona un utente con cui effettuare l’accesso:");
        for (Utente u : elencoUtenti.values()) {
            System.out.printf("%d - %s %s (%s)%n", u.getIdUtente(), u.getNome(), u.getCognome(), u.getRuolo());
        }

        int idUtenteSelezionato = -1;
        while (true) {
            System.out.print("Inserisci l’ID utente: ");
            String input = scanner.nextLine();
            try {
                idUtenteSelezionato = Integer.parseInt(input);
                if (elencoUtenti.containsKey(idUtenteSelezionato)) break;
                else System.out.println("ID utente non valido, riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Input non valido, inserisci un numero.");
            }
        }

        System.out.println("Utente " + elencoUtenti.get(idUtenteSelezionato).getNome() + " selezionato.");
        return idUtenteSelezionato;
    }

    public void avviaPrenotazione(int idUtente) {
        Utente utenteCorrente = elencoUtenti.get(idUtente);
        prenotazioneInCorso = new Prenotazione(utenteCorrente);
    }

    public void selezionaParametriDaConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nSeleziona una delle seguenti sedi disponibili:");

        for (Sede sede : elencoSedi.values()) {
            System.out.println("ID: " + sede.getIdSede() + " - " + sede.getNome() + " (" + sede.getIndirizzo() + ")");
        }

        int idSedeSelezionata = -1;
        while (true) {
            System.out.print("Inserisci l'ID della sede desiderata: ");
            String inputSede = scanner.nextLine();
            try {
                idSedeSelezionata = Integer.parseInt(inputSede);
                if (elencoSedi.containsKey(idSedeSelezionata)) {
                    break;
                } else {
                    System.out.println("ID sede non valido. Riprova.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Formato non valido. Inserisci un numero intero.");
            }
        }

        // Data in formato yyyy-MM-dd
        LocalDate dataPrenotazione = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print("\nInserisci la data di prenotazione (formato yyyy-MM-dd, es. 2025-07-13): ");
            String inputData = scanner.nextLine();
            try {
                dataPrenotazione = LocalDate.parse(inputData, formatter);
                break;
            } catch (DateTimeParseException e) {
                System.out.println("Data non valida. Usa il formato yyyy-MM-dd.");
            }
        }

        // Fascia oraria nel formato HH:mm-HH:mm
        String fasciaOraria = null;
        Pattern fasciaPattern = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)-([01]\\d|2[0-3]):([0-5]\\d)$");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        while (true) {
            System.out.print("\nInserisci la fascia oraria nel formato HH:mm-HH:mm (es. 17:00-18:00): ");
            String inputFascia = scanner.nextLine();

            if (!fasciaPattern.matcher(inputFascia).matches()) {
                System.out.println("Formato fascia oraria non valido. Usa il formato HH:mm-HH:mm.");
                continue;
            }

            String[] parts = inputFascia.split("-");
            try {
                LocalTime inizio = LocalTime.parse(parts[0], timeFormatter);
                LocalTime fine = LocalTime.parse(parts[1], timeFormatter);

                if (fine.isAfter(inizio)) {
                    fasciaOraria = inputFascia;
                    break;
                } else {
                    System.out.println("L'ora di fine deve essere successiva all'ora di inizio.");
                }
            } catch (DateTimeParseException e) {
                System.out.println("Orario non valido. Usa il formato HH:mm.");
            }
        }

        selezionaParametri(idSedeSelezionata, dataPrenotazione, fasciaOraria);
        System.out.println("Parametri di prenotazione selezionati con successo.");
    }

    public void selezionaParametri(int idSede, LocalDate data, String fascia) {
        prenotazioneInCorso.setSede(elencoSedi.get(idSede));
        prenotazioneInCorso.setData(data);
        prenotazioneInCorso.setFasciaOraria(fascia);
    }

    public void selezionaFiltriDaConsole() {
        Scanner scanner = new Scanner(System.in);

        // 1. Filtro vicino alla finestra
        boolean vicinoFinestra = false;
        while (true) {
            System.out.print("\nDesideri una postazione vicino alla finestra? (s/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("s")) {
                vicinoFinestra = true;
                break;
            } else if (input.equals("n")) {
                vicinoFinestra = false;
                break;
            } else {
                System.out.println("Risposta non valida. Inserisci 's' per sì o 'n' per no.");
            }
        }

        // 2. Filtro dotazione
        System.out.println("\nDotazioni disponibili:");
        for (Dotazione d : elencoDotazioni.values()) {
            System.out.println("ID: " + d.getIdDotazione() + " - " + d.getNome() + " (" + d.getDescrizione() + ")");
        }

        int idDotazione = -1;
        while (true) {
            System.out.print("Inserisci l'ID della dotazione richiesta (o -1 per nessuna preferenza): ");
            String input = scanner.nextLine().trim();
            try {
                idDotazione = Integer.parseInt(input);
                if (idDotazione == -1 || elencoDotazioni.containsKey(idDotazione)) {
                    break;
                } else {
                    System.out.println("ID dotazione non valido. Riprova.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Formato non valido. Inserisci un numero intero.");
            }
        }

        // 3. Filtro area
        Sede sedeSelezionata = prenotazioneInCorso.getSede();
        Map<Integer, Area> elencoAree = sedeSelezionata.getElencoAree();

        System.out.println("\nAree disponibili nella sede \"" + sedeSelezionata.getNome() + "\":");
        for (Area area : elencoAree.values()) {
            System.out.println("ID: " + area.getIdArea() + " - " + area.getNome() + " (" + area.getDescrizione() + ")");
        }

        int idArea = -1;
        while (true) {
            System.out.print("Inserisci l'ID dell'area desiderata (o -1 per nessuna preferenza): ");
            String input = scanner.nextLine().trim();
            try {
                idArea = Integer.parseInt(input);
                if (idArea == -1 || elencoAree.containsKey(idArea)) {
                    break;
                } else {
                    System.out.println("ID area non valido. Riprova.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Formato non valido. Inserisci un numero intero.");
            }
        }

        // Applica i filtri
        selezionaFiltri(vicinoFinestra, idDotazione, idArea);

        System.out.println("\nFiltri selezionati:");
        System.out.println("- Vicino alla finestra: " + (vicinoFinestra ? "Sì" : "No"));
        System.out.println("- Dotazione: " + (idDotazione == -1 ? "Nessuna preferenza" : elencoDotazioni.get(idDotazione).getNome()));
        System.out.println("- Area: " + (idArea == -1 ? "Nessuna preferenza" : elencoAree.get(idArea).getNome()));
    }

    public void selezionaFiltri(boolean vicinoFinestra, int idDotazione, int idArea) {
        prenotazioneInCorso.setVicinoFinestra(vicinoFinestra);
        prenotazioneInCorso.setIdDotazione(idDotazione);
        prenotazioneInCorso.setIdArea(idArea);
    }

    public List<Postazione> mostraPostazioniDisponibili() {
        List<Postazione> postazioniDisponibili = new ArrayList<>();
        Sede sede = prenotazioneInCorso.getSede();

        for (Area area : sede.getElencoAree().values()) {
            for (Postazione p : area.getElencoPostazioni().values()) {
                if (prenotazioneInCorso.verificaParametri(p)) {
                    postazioniDisponibili.add(p);
                }
            }
        }

        System.out.println("\nPostazioni disponibili: ");
        for (Postazione p : postazioniDisponibili) {
            System.out.println(p);
        }

        return postazioniDisponibili;
    }

    public void selezionaPostazioneDaConsole() {
        List<Postazione> disponibili = mostraPostazioniDisponibili();

        if (disponibili.isEmpty()) {
            System.out.println("Nessuna postazione disponibile per i criteri selezionati.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        int idSelezionato = -1;

        while (true) {
            System.out.print("\nInserisci l'ID della postazione da prenotare: ");
            String input = scanner.nextLine().trim();

            try {
                idSelezionato = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Inserisci un numero intero valido.");
                continue;
            }

            Postazione selezionata = null;
            for (Postazione p : disponibili) {
                if (p.getIdPostazione() == idSelezionato) {
                    selezionata = p;
                    break;
                }
            }

            if (selezionata != null) {
                try {
                    selezionaPostazione(selezionata); // eventuale errore qui
                    break;
                } catch (Exception e) {
                    System.out.println("⚠️ Errore durante la selezione della postazione: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("❌ ID non valido. Riprova.");
            }
        }

    }

    public void selezionaPostazione(Postazione p) {
        this.postazioneSelezionata = p;
        prenotazioneInCorso.setPostazione(p);

        // Recupera i dati per il controllo delle regole
        int idSede = p.getArea().getSede().getIdSede();
        int idUtente = prenotazioneInCorso.getUtente().getIdUtente();
        LocalDate data = prenotazioneInCorso.getData();

        // Ottiene tutte le regole applicabili
        List<Regola> regoleApplicabili = Regola.mostraRegole(idSede, idUtente, data);

        // Applica le regole alla prenotazione
        for (Regola r : regoleApplicabili) {
            prenotazioneInCorso.applicaRegola(r);
        }

        System.out.println("\n✅ Postazione selezionata correttamente!");
        if (!regoleApplicabili.isEmpty()) {
            System.out.println("\n⚠️ Sono state applicate le seguenti regole:");
            for (Regola r : regoleApplicabili) {
                System.out.println("- " + r.getDescrizione());
            }
        }

        System.out.println("\n📋 Riepilogo prenotazione:");
        System.out.println(prenotazioneInCorso);
    }

    public void confermaPrenotazione() {
        Prenotazione p = this.prenotazioneInCorso;

        int idUtente = p.getUtente().getIdUtente();
        int idSede = p.getSede().getIdSede();
        LocalDate data = p.getData();
        String fascia = p.getFasciaOraria();
        int idPostazione = p.getPostazione().getIdPostazione();

        try {
            // 1. Verifica se l’utente non ha blocchi ed è prenotabile per quella data/sede
            Regola.verificaPrenotabilità(p);

            // 2. Verifica se la postazione è ancora disponibile
            if (!p.verificaDisponibilità(idPostazione, data, fascia)) {
                throw new IllegalStateException("⛔ La postazione selezionata non è più disponibile.");
            }

            // 3. Registra prenotazione
            Prenotazione.registraPrenotazione(p);

            System.out.println("\n✅ Prenotazione confermata con successo!");
            System.out.println("📄 Ricevuta: " + p);

        } catch (IllegalStateException e) {
            throw e;
        }
    }

}
