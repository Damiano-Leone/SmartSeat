package com.leone.app.domain;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
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

    private Prenotazione prenotazioneInCorso;


    public SmartSeat(){
        this.elencoSedi = new HashMap<>();
        this.elencoUtenti = new HashMap<>();
        this.elencoDotazioni = new HashMap<>();

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

    public Map<Integer, Utente> getElencoUtenti() {
        return elencoUtenti;
    }

    public Map<Integer, Dotazione> getElencoDotazioni() {
        return elencoDotazioni;
    }

    public Map<Integer, Sede> getElencoSedi() {
        return elencoSedi;
    }

    public Prenotazione getPrenotazioneInCorso() {
        return prenotazioneInCorso;
    }

    public void setPrenotazioneInCorso(Prenotazione prenotazione) {
        this.prenotazioneInCorso = prenotazione;
    }

    public void loadUtenti() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Utenti.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;
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

    public Utente selezionaUtenteDaConsole(String ruoloRichiesto) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Seleziona un utente con cui effettuare l’accesso:");
        for (Utente u : elencoUtenti.values()) {
            System.out.printf("%d - %s %s (%s)%n", u.getIdUtente(), u.getNome(), u.getCognome(), u.getRuolo());
        }

        Utente utenteSelezionato = null;
        while (utenteSelezionato == null) {
            System.out.print("Inserisci l’ID utente: ");
            String input = scanner.nextLine();
            int idUtente;
            try {
                idUtente = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ Input non valido, inserisci un numero.");
                continue;
            }

            Utente u = elencoUtenti.get(idUtente);
            if (u == null) {
                System.out.println("❌ ID utente non valido, riprova.");
                continue;
            }

            if (!u.getRuolo().equalsIgnoreCase(ruoloRichiesto)) {
                System.out.println("❌ Accesso negato. L’utente selezionato non ha il ruolo richiesto: " + ruoloRichiesto);
                continue;
            }

            utenteSelezionato = u;
        }

        System.out.println("✅ Utente " + utenteSelezionato.getNome() + " " + utenteSelezionato.getCognome() + " selezionato con successo.");
        return utenteSelezionato;
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

                // Controllo che la data non sia passata
                if (dataPrenotazione.isBefore(LocalDate.now())) {
                    System.out.println("La data inserita è già passata. Inserisci una data futura o odierna.");
                    continue;
                }

                break;
            } catch (DateTimeParseException e) {
                System.out.println("Data non valida. Usa il formato yyyy-MM-dd.");
            }
        }

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

                if (!fine.isAfter(inizio)) {
                    System.out.println("L'ora di fine deve essere successiva all'ora di inizio.");
                    continue;
                }

                // Controllo che l'orario non sia già passato se la data è oggi
                if (dataPrenotazione.isEqual(LocalDate.now()) && inizio.isBefore(LocalTime.now())) {
                    System.out.println("L'orario di inizio è già passato. Inserisci un orario valido.");
                    continue;
                }

                fasciaOraria = inputFascia;
                break;

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
            System.out.print("\nVuoi applicare il filtro 'vicino alla finestra'? (s = mostra solo postazioni vicino alla finestra, n = mostra tutte le postazioni): ");
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
        System.out.println("- Vicino alla finestra: " + (vicinoFinestra ? "Sì" : "Nessuna preferenza"));
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

        List<Postazione> elencoPostazioni = sede.getPostazioni();

        for (Postazione p : elencoPostazioni) {
            if (prenotazioneInCorso.verificaParametri(p)) {
                postazioniDisponibili.add(p);
            }
        }

        System.out.println("\nPostazioni disponibili: ");
        for (Postazione p : postazioniDisponibili) {
            System.out.println(p);
        }

        return postazioniDisponibili;
    }

    public boolean selezionaPostazioneDaConsole() {
        List<Postazione> disponibili = mostraPostazioniDisponibili();

        if (disponibili.isEmpty()) {
            System.out.println("⚠️ Nessuna postazione disponibile per i criteri selezionati.");

            // Dopo aver modificato parametri e filtri, ritorna alla scelta della postazione
            selezionaParametriDaConsole();
            selezionaFiltriDaConsole();
            return selezionaPostazioneDaConsole();
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
                boolean ok = selezionaPostazione(selezionata);
                if (ok) {
                    return true; // selezione valida
                } else {
                    if (gestisciRegoleNonRispettate()) {
                        // L'utente vuole ripetere la prenotazione
                        prenotazioneInCorso.resetRegole();
                        selezionaParametriDaConsole();
                        selezionaFiltriDaConsole();
                        return selezionaPostazioneDaConsole();
                    } else {
                        // L'utente vuole tornare al menu
                        System.out.println("🔙 Ritorno al menu principale...");
                        return false;
                    }
                }
            } else {
                System.out.println("❌ ID non valido. Riprova.");
            }
        }

    }

    public boolean selezionaPostazione(Postazione p) {
        try {
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
                prenotazioneInCorso.aggiungiRegola(r);
            }

            System.out.println("\n✅ Postazione selezionata correttamente!");
            if (!regoleApplicabili.isEmpty()) {
                System.out.println("\n⚠️ Sono state applicate le seguenti regole:");
                for (Regola r : regoleApplicabili) {
                    System.out.println("- " + r.getDescrizione());
                }
            }

            System.out.println("\n📋 Riepilogo prenotazione:");
            System.out.println(prenotazioneInCorso.riepilogoPrenotazione());
            return true;
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            System.out.println(msg);
            return false;
        }
    }

    public void confermaPrenotazioneDaConsole() {
        Scanner scanner = new Scanner(System.in);
        String input;
        while (true) {
            System.out.print("\nVuoi confermare la prenotazione? (s/n): ");
            input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("s") || input.equals("n")) {
                break; // input valido
            } else {
                System.out.println("⚠️ Inserisci un valore valido: 's' per sì oppure 'n' per no.");
            }
        }

        if (input.equals("n")) {
            System.out.println("❌ Prenotazione annullata.");
            return;
        }
        confermaPrenotazione();
    }

    public void confermaPrenotazione() {
        Prenotazione p = this.prenotazioneInCorso;

        LocalDate data = p.getData();
        String fascia = p.getFasciaOraria();
        int idPostazione = p.getPostazione().getIdPostazione();

        try {
            // 1. Verifica se l’utente non ha blocchi ed è prenotabile per quella data/sede
            Regola.verificaPrenotabilità(p);

            // 2. Verifica se la postazione è ancora disponibile
            if (!p.verificaDisponibilità(idPostazione, data, fascia)) {
                System.out.println("⛔ La postazione selezionata non è più disponibile. Selezionane un'altra.");

                // Torna alla selezione della postazione
                selezionaPostazioneDaConsole();
                confermaPrenotazioneDaConsole();
                return;
            }

            // 3. Registra prenotazione
            String qrPath = p.registraPrenotazione();

            System.out.println("\n✅ Prenotazione confermata con successo!");
            System.out.println("📄 Ricevuta: " + p);
            System.out.println("🔗 QR Code generato: " + qrPath);

            Notifica.generaNotificaPrenotazione(p, qrPath, false);

        } catch (IllegalStateException e) {
            String msg = e.getMessage();

            // Altrimenti (altre regole non rispettate)
            System.out.println(msg);

            if (gestisciRegoleNonRispettate()) {
                // L'utente vuole ripetere la prenotazione
                prenotazioneInCorso.resetRegole();
                selezionaParametriDaConsole();
                selezionaFiltriDaConsole();
                selezionaPostazioneDaConsole();
                confermaPrenotazioneDaConsole();
            } else {
                // L'utente vuole tornare al menu
                System.out.println("🔙 Ritorno al menu principale...");
            }
        }
    }

    private boolean gestisciRegoleNonRispettate() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n⚠️ Alcune regole non sono rispettate.");
            System.out.println("Vuoi ripetere la prenotazione o terminare?");
            System.out.println("1. Ripeti prenotazione");
            System.out.println("0. Torna al menu principale");
            System.out.print("Scelta: ");

            String scelta = scanner.nextLine().trim();

            if (scelta.equals("1")) {
                return true;  // ripeti scelta dei parametri
            } else if (scelta.equals("0")) {
                return false; // torna al menu
            } else {
                System.out.println("❌ Scelta non valida. Inserisci 1 o 0.");
            }
        }
    }

    public void effettuaCheckIn() {
        try {
            // 1. L'utente seleziona un file immagine (QR code)
            String basePath = System.getProperty("user.dir");
            File qrFolder = new File(basePath, "qrcode");
            JFileChooser fileChooser = new JFileChooser(qrFolder);
            JFrame frame = new JFrame();
            frame.setAlwaysOnTop(true);
            frame.setVisible(false);
            fileChooser.setDialogTitle("Seleziona un QR Code da aprire");
            int result = fileChooser.showOpenDialog(frame);
            if (result != JFileChooser.APPROVE_OPTION) {
                System.out.println("❌ Nessun file selezionato.");
                return;
            }

            File qrFile = fileChooser.getSelectedFile();

            // 2. Decodifica QR code
            BufferedImage bufferedImage = ImageIO.read(qrFile);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result resultQR = new MultiFormatReader().decode(bitmap);

            String qrCode = resultQR.getText(); // testo contenuto nel QR
            System.out.println("📸 QR Code letto: " + qrCode);

            // 3. Recupera prenotazione
            Prenotazione pren = Prenotazione.trovaPrenotazione(qrCode);

            // 4. Crea CheckIn associando i dati della prenotazione
            CheckIn checkInCorrente = new CheckIn(
                    pren.getUtente().getIdUtente(),
                    pren.getId(),
                    pren.getPostazione().getIdPostazione(),
                    "QR"
            );

            // 5. Registra il CheckIn
            if (checkInCorrente.registraCheckIn()) {
                System.out.println("✅ Check-in effettuato con successo per la prenotazione: " + pren.getId());
            } else {
                System.out.println("⚠️ Errore durante il salvataggio del check-in.");
            }

        } catch (IllegalStateException e) {
            System.out.println("⛔ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("⚠️ Errore durante il check-in: " + e.getMessage());
        }
    }

    // usato solamente per i test
    public boolean effettuaCheckInDaFile(File qrFile) {
        try {
            BufferedImage bufferedImage = ImageIO.read(qrFile);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result resultQR = new MultiFormatReader().decode(bitmap);
            String qrCode = resultQR.getText();

            Prenotazione pren = Prenotazione.trovaPrenotazione(qrCode);
            CheckIn checkIn = new CheckIn(
                    pren.getUtente().getIdUtente(),
                    pren.getId(),
                    pren.getPostazione().getIdPostazione(),
                    "QR"
            );
            return checkIn.registraCheckIn();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<Regola> avviaConfigurazioneRegole() {
        System.out.println("\n📋 Elenco regole:");
        List<Regola> regole = Regola.mostraTutteLeRegole();
        if (regole.isEmpty()) {
            System.out.println("Nessuna regola configurata.");
        } else {
            System.out.printf(
                    "%-3s | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                    "ID", "Nome", "Tipo", "Scope", "Rif", "Valore", "Inizio", "Fine", "Descrizione"
            );
            System.out.println("─".repeat(170));

            for (Regola r : regole) {
                System.out.printf(
                        "%-3d | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                        r.getId(),
                        r.getNome(),
                        r.getTipo(),
                        r.getScope(),
                        (r.getIdRiferimento() == -1 ? "*" : r.getIdRiferimento()),
                        (r.getValore() == null || r.getValore().isBlank() ? "-" : r.getValore()),
                        r.getDataInizio(),
                        r.getDataFine(),
                        r.getDescrizione()
                );
            }
        }
        return regole;
    }

    public void gestisciRegoleDaConsole() {
        Scanner sc = new Scanner(System.in);
        List<Regola> tutte = avviaConfigurazioneRegole();
        int scelta = 0;
        while (scelta < 1 || scelta > 3) {
            System.out.print("\nVuoi (1) aggiungere, (2) modificare o (3) eliminare una regola?: ");
            try {
                scelta = Integer.parseInt(sc.nextLine());
                if (scelta < 1 || scelta > 3) {
                    System.out.println("❌ Scelta non valida, inserisci 1, 2 o 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Inserisci un numero valido (1, 2 o 3).");
            }
        }

        Regola regolaCorrente;
        if (scelta == 3) {
            if (tutte == null || tutte.isEmpty()) {
                System.out.println("❌ Nessuna regola disponibile.");
                return;
            }

            System.out.print("ID regola da eliminare: ");
            int idDel;
            try {
                idDel = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("❌ Inserisci un numero intero valido.");
                return;
            }

            regolaCorrente = tutte.stream()
                    .filter(r -> r.getId() == idDel)
                    .findFirst()
                    .orElse(null);

            if (regolaCorrente == null) {
                System.out.println("❌ ID regola non trovato.");
                return;
            }

            annullaRegola(regolaCorrente);
            return;
        }

        if (scelta == 2) {
            if (tutte == null || tutte.isEmpty()) {
                System.out.println("❌ Nessuna regola disponibile, impossibile modificare.");
                return;
            }
            System.out.print("Inserisci l'ID della regola da modificare: ");
            int id = Integer.parseInt(sc.nextLine());

            Regola esistente = null;
            for (Regola r : tutte) {
                if (r.getId() == id) {
                    esistente = r;
                    break;
                }
            }

            if (esistente == null) {
                System.out.println("❌ Nessuna regola trovata con ID " + id);
                return;
            }
            // Stampa intestazione + riga della regola selezionata
            System.out.printf(
                    "\n%-3s | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                    "ID", "Nome", "Tipo", "Scope", "Rif", "Valore", "Inizio", "Fine", "Descrizione"
            );

            System.out.printf(
                    "%-3d | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                    esistente.getId(),
                    esistente.getNome(),
                    esistente.getTipo(),
                    esistente.getScope(),
                    (esistente.getIdRiferimento() == -1 ? "*" : esistente.getIdRiferimento()),
                    (esistente.getValore() == null || esistente.getValore().isBlank() ? "-" : esistente.getValore()),
                    esistente.getDataInizio(),
                    esistente.getDataFine(),
                    esistente.getDescrizione()
            );
            System.out.println("\nPremi INVIO per mantenere il valore attuale, oppure scrivi un nuovo valore.");
            System.out.print("Nome regola [" + esistente.getNome() + "]: ");
            String nuovoNome = sc.nextLine().trim();
            if (!nuovoNome.isEmpty()) {
                esistente.setNome(nuovoNome);
            }

            // Valore (solo se tipo = LIMITE_ORE_GIORNALIERO o BLOCCO_GIORNI)
            switch (esistente.getTipo()) {
                case LIMITE_ORE_GIORNALIERO:
                    while (true) {
                        System.out.print("Valore (ore 1-23) [" + esistente.getValore() + "]: ");
                        String nuovoValoreOre = sc.nextLine().trim();

                        // INVIO → mantieni valore attuale
                        if (nuovoValoreOre.isEmpty()) {
                            break;
                        }

                        try {
                            int ore = Integer.parseInt(nuovoValoreOre);
                            if (ore >= 1 && ore <= 23) {
                                esistente.setValore(nuovoValoreOre);
                                break;
                            } else {
                                System.out.println("❌ Numero non valido. Inserisci un valore tra 1 e 23 oppure premi INVIO per mantenere.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("\n❌ Inserisci un numero intero valido oppure premi INVIO per mantenere.");
                        }
                    }
                    break;

                case BLOCCO_GIORNI:
                    while (true) {
                        System.out.print("Valore (giorni separati da ',') [" + esistente.getValore() + "]: ");
                        String nuovoValoreGiorni = sc.nextLine().trim().toUpperCase();

                        // INVIO → mantieni valore attuale
                        if (nuovoValoreGiorni.isEmpty()) {
                            break;
                        }

                        Set<String> giorniValidi = Set.of(
                                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                                "FRIDAY", "SATURDAY", "SUNDAY"
                        );
                        String[] giorni = nuovoValoreGiorni.split(",");
                        boolean validi = true;
                        for (String g : giorni) {
                            if (!giorniValidi.contains(g.trim())) {
                                validi = false;
                                break;
                            }
                        }
                        if (validi) {
                            esistente.setValore(String.join(",", giorni));
                            break;
                        } else {
                            System.out.println("❌ Giorni non validi. Usa nomi in inglese separati da ',' oppure premi INVIO per mantenere.");
                        }
                    }
                    break;

                default:
                    // per altri tipi, il valore non si modifica
                    break;
            }

            // Date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Data inizio
            LocalDate dataInizioValida = null;
            do {
                System.out.print("Data inizio (yyyy-MM-dd) [" + esistente.getDataInizio() + "]: ");
                String nuovaDataInizio = sc.nextLine().trim();

                if (nuovaDataInizio.isEmpty()) {
                    dataInizioValida = esistente.getDataInizio(); // mantiene la data esistente
                    break;
                }

                try {
                    LocalDate nuova = LocalDate.parse(nuovaDataInizio, formatter);
                    if (!nuova.isBefore(LocalDate.now())) { // oggi o successiva
                        dataInizioValida = nuova;
                    } else {
                        System.out.println("❌ La data deve essere oggi o successiva. Riprova.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Formato non valido. Riprova.");
                }
            } while (dataInizioValida == null);

            esistente.setDataInizio(dataInizioValida);

            // Data fine
            LocalDate dataFineValida = null;
            do {
                System.out.print("Data fine (yyyy-MM-dd) [" + esistente.getDataFine() + "]: ");
                String nuovaDataFine = sc.nextLine().trim();

                if (nuovaDataFine.isEmpty()) {
                    dataFineValida = esistente.getDataFine(); // mantiene la data esistente
                    break;
                }

                try {
                    LocalDate nuova = LocalDate.parse(nuovaDataFine, formatter);
                    if (!nuova.isBefore(esistente.getDataInizio())) { // deve essere >= data inizio
                        dataFineValida = nuova;
                    } else {
                        System.out.println("❌ Data fine non può precedere data inizio. Riprova.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Formato non valido. Riprova.");
                }
            } while (dataFineValida == null);

            esistente.setDataFine(dataFineValida);


            // Descrizione
            System.out.print("Descrizione [" + esistente.getDescrizione() + "]: ");
            String nuovaDescrizione = sc.nextLine().trim();
            if (!nuovaDescrizione.isEmpty()) {
                esistente.setDescrizione(nuovaDescrizione);
            }

            // Usa l'oggetto aggiornato come regolaCorrente
            regolaCorrente = esistente;
        }
        else {
            System.out.println("Inserisci i dati della nuova regola:");
            // Nome regola
            String nome;
            while (true) {
                System.out.print("Nome regola: ");
                nome = sc.nextLine().trim();
                if (!nome.isEmpty()) {
                    break; // valido → esco dal ciclo
                }
                System.out.println("❌ Il nome della regola non può essere vuoto. Riprova.");
            }

            Regola.TipoRegola tipo = null;
            while (tipo == null) {
                System.out.print("Tipo (LIMITE_ORE_GIORNALIERO, BLOCCO_UTENTE, BLOCCO_GIORNI): ");
                String inputTipo = sc.nextLine().trim().toUpperCase();
                try {
                    tipo = Regola.TipoRegola.valueOf(inputTipo);
                } catch (IllegalArgumentException e) {
                    System.out.println("❌ Tipo non valido. Valori ammessi: LIMITE_ORE_GIORNALIERO, BLOCCO_UTENTE, BLOCCO_GIORNI.");
                }
            }

            Regola.ScopeRegola scope = null;
            if (tipo == Regola.TipoRegola.BLOCCO_UTENTE) {
                scope = Regola.ScopeRegola.UTENTE;
                System.out.println("Scope impostato automaticamente su UTENTE per il tipo BLOCCO_UTENTE.");
            } else {
                while (scope == null) {
                    System.out.print("Scope (SEDE, UTENTE, GLOBALE): ");
                    String inputScope = sc.nextLine().trim().toUpperCase();
                    try {
                        scope = Regola.ScopeRegola.valueOf(inputScope);
                    } catch (IllegalArgumentException e) {
                        System.out.println("❌ Scope non valido. Valori ammessi: SEDE, UTENTE, GLOBALE.");
                    }
                }
            }

            int idRif;

            if (scope == Regola.ScopeRegola.SEDE) {
                while (true) {
                    System.out.print("ID riferimento SEDE: ");
                    String input = sc.nextLine();
                    try {
                        idRif = Integer.parseInt(input);
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Inserisci un numero valido per l'ID SEDE.");
                    }
                }
            } else if (scope == Regola.ScopeRegola.UTENTE) {
                while (true) {
                    System.out.print("ID riferimento UTENTE: ");
                    String input = sc.nextLine();
                    try {
                        idRif = Integer.parseInt(input);
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Inserisci un numero valido per l'ID UTENTE.");
                    }
                }

            } else {
                // Per GLOBALE, non chiediamo nulla: assegniamo -1
                idRif = -1;
            }

            String valore = "";

            switch (tipo) {
                case BLOCCO_UTENTE:
                    valore = ""; // valore vuoto, non serve inserimento
                    break;

                case LIMITE_ORE_GIORNALIERO:
                    while (true) {
                        System.out.print("Valore (numero intero da 1 a 23 ore): ");
                        String input = sc.nextLine().trim();
                        try {
                            int ore = Integer.parseInt(input);
                            if (ore >= 1 && ore <= 23) {
                                valore = input;
                                break;
                            } else {
                                System.out.println("❌ Inserisci un numero valido tra 1 e 23.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("❌ Inserisci un numero intero valido.");
                        }
                    }
                    break;

                case BLOCCO_GIORNI:
                    Set<String> giorniValidi = Set.of(
                            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
                    );
                    while (true) {
                        System.out.print("Valore (giorni separati da ',' es. SATURDAY,SUNDAY): ");
                        String input = sc.nextLine().trim().toUpperCase();
                        String[] giorni = input.split(",");
                        boolean validi = true;
                        for (String g : giorni) {
                            if (!giorniValidi.contains(g.trim())) {
                                validi = false;
                                break;
                            }
                        }
                        if (validi) {
                            valore = String.join(",", giorni);
                            break;
                        } else {
                            System.out.println("❌ Giorni non validi. Usa nomi dei giorni in inglese separati da ',' (es. MONDAY,TUESDAY).");
                        }
                    }
                    break;

                default:
                    System.out.print("Valore: ");
                    valore = sc.nextLine();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dataInizio = null;
            while (dataInizio == null) {
                System.out.print("Data inizio (yyyy-MM-dd): ");
                String input = sc.nextLine().trim();
                try {
                    LocalDate parsed = LocalDate.parse(input, formatter);
                    if (!parsed.isBefore(LocalDate.now())) { // controlla che sia oggi o dopo
                        dataInizio = parsed;
                    } else {
                        System.out.println("❌ La data inizio deve essere oggi o successiva.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Formato non valido. Inserisci la data nel formato yyyy-MM-dd.");
                }
            }

            LocalDate dataFine = null;
            while (dataFine == null) {
                System.out.print("Data fine (yyyy-MM-dd): ");
                String input = sc.nextLine().trim();
                try {
                    dataFine = LocalDate.parse(input, formatter);
                    if (dataFine.isBefore(dataInizio)) { // data fine >= data inizio
                        System.out.println("❌ La data fine non può essere precedente alla data inizio.");
                        dataFine = null;
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Formato non valido. Inserisci la data nel formato yyyy-MM-dd.");
                }
            }

            // Descrizione
            String descrizione;
            while (true) {
                System.out.print("Descrizione: ");
                descrizione = sc.nextLine().trim();
                if (!descrizione.isEmpty()) {
                    break;
                }
                System.out.println("❌ La descrizione non può essere vuota. Riprova.");
            }
            regolaCorrente = new Regola(-1, nome, tipo, scope, idRif, valore, dataInizio, dataFine, descrizione);
        }
        boolean completato = false;
        while (!completato) {
            if (confermaRegola(regolaCorrente)) {
                completato = true; // regola salvata → esco
            } else {
                gestisciRegoleDaConsole();
                return;
            }
        }
    }

    public boolean confermaRegola(Regola regolaCorrente) {
        // Valida coerenza della regola
        if (!regolaCorrente.validaCoerenzaRegola()) {
            System.out.println("❌ Regola non coerente: modifica i valori o elimina eventuali regole conflittuali.");
            return false;
        }
        if (regolaCorrente.registraRegola()) {
            System.out.println("✅ Registrazione regola effettuata con successo!");
        } else {
            System.out.println("❌ Registrazione regola fallita.");
            return false;
        }

        List<Prenotazione> tuttePrenotazioni = Prenotazione.mostraTutteLePrenotazioni();
        // Iterazione inversa: mantengo le prime prenotazioni ed eventualmente annullo solo le ultime
        for (int i = tuttePrenotazioni.size() - 1; i >= 0; i--) {
            Prenotazione p = tuttePrenotazioni.get(i);
            // Solo prenotazioni con data >= dataInizio della regola
            if (p.verificaConflittoPrenotazione(regolaCorrente) && !CheckIn.checkInGiaEffettuato(p.getId())) {
                // Conflitto rilevato → annulla prenotazione e notifica utente
                System.out.println("⚠️ Conflitto rilevato: Prenotazione [ID:" + p.getId() +
                        "] | Utente:" + p.getUtente().getNome() + " " + p.getUtente().getCognome() +
                        " | Data:" + p.getData() +
                        " | Orario:" + p.getFasciaOraria() +
                        " | Postazione:" + p.getPostazione().getCodice() + " " + p.getPostazione().getPosizione());
                if (p.annullaPrenotazione()) {
                    System.out.println("✅ Prenotazione ID [" + p.getId() + "] è stata annullata correttamente.");
                } else {
                    System.out.println("❌ Errore durante l'annullamento della prenotazione ID " + p.getId());
                    return false;
                }
                try {
                    Notifica.generaNotificaAnnullamento(p, regolaCorrente, false);
                } catch (Exception e) {
                    System.out.println("❌ Errore nell'invio email di notifica a " + p.getUtente().getEmail());
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    public boolean annullaRegola(Regola regolaCorrente) {
        if (regolaCorrente == null) return false;

        System.out.println("⚠️ Stai per eliminare la regola: \"" + regolaCorrente.getNome() + "\"");
        System.out.println("\nDettagli:");
        System.out.printf(
                "%-3s | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                "ID", "Nome", "Tipo", "Scope", "Rif", "Valore", "Inizio", "Fine", "Descrizione"
        );
        System.out.printf(
                "%-3d | %-23s | %-22s | %-7s | %-3s | %-30s | %-10s | %-10s | %-40s%n",
                regolaCorrente.getId(),
                regolaCorrente.getNome(),
                regolaCorrente.getTipo(),
                regolaCorrente.getScope(),
                (regolaCorrente.getIdRiferimento() == -1 ? "*" : regolaCorrente.getIdRiferimento()),
                (regolaCorrente.getValore() == null || regolaCorrente.getValore().isBlank() ? "-" : regolaCorrente.getValore()),
                regolaCorrente.getDataInizio(),
                regolaCorrente.getDataFine(),
                regolaCorrente.getDescrizione()
        );

        Scanner sc = new Scanner(System.in);
        System.out.print("\nConfermi l’eliminazione della regola? (s/n): ");
        String conferma = sc.nextLine().trim().toUpperCase();
        if (!conferma.equals("S")) {
            System.out.println("❌ Operazione annullata.");
            return false;
        }

        if (regolaCorrente.eliminaRegola()) {
            System.out.println("✅ Regola eliminata con successo!");
            return true;
        } else {
            System.out.println("❌ Errore durante l’eliminazione della regola.");
            return false;
        }
    }
}
