package com.leone.app.domain;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Regola {
    private int id;
    private String nome;
    private TipoRegola tipo;
    private ScopeRegola scope;
    private int idRiferimento; // sede o utente
    private String valore;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private String descrizione;

    public enum TipoRegola {
        LIMITE_ORE_GIORNALIERO,
        BLOCCO_UTENTE,
        BLOCCO_GIORNI
    }

    public enum ScopeRegola {
        SEDE, UTENTE, GLOBALE
    }

    private static String FILE_NAME = "Regole.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    private static int nextId = 1;

    public Regola(int id, String nome, TipoRegola tipo, ScopeRegola scope, int idRiferimento, String valore, LocalDate dataInizio, LocalDate dataFine, String descrizione) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.scope = scope;
        this.idRiferimento = idRiferimento;
        this.valore = valore;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.descrizione = descrizione;
    }

    public boolean isAttivaPer(int idSede, int idUtente, LocalDate data) {
        if (data.isBefore(dataInizio) || data.isAfter(dataFine)) return false;

        return switch (scope) {
            case SEDE -> idRiferimento == idSede;
            case UTENTE -> idRiferimento == idUtente;
            case GLOBALE -> true;
        };
    }

    public static List<Regola> mostraRegole(int idSede, int idUtente, LocalDate data) {
        List<Regola> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;
                String[] parts = line.split(";");
                int id = Integer.parseInt(parts[0]);
                String nome = parts[1];
                TipoRegola tipo = TipoRegola.valueOf(parts[2]);
                ScopeRegola scope = ScopeRegola.valueOf(parts[3]);
                int idRif = parts[4].equals("*") ? -1 : Integer.parseInt(parts[4]);
                String valore = parts[5];
                LocalDate dataInizio = LocalDate.parse(parts[6]);
                LocalDate dataFine = LocalDate.parse(parts[7]);
                String descrizione = parts[8];

                Regola r = new Regola(id, nome, tipo, scope, idRif, valore, dataInizio, dataFine, descrizione);

                if (r.isAttivaPer(idSede, idUtente, data)) {
                    result.add(r);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore lettura file regole: " + e.getMessage());
        }
        return result;
    }

    public static List<Regola> mostraTutteLeRegole() {
        List<Regola> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;
                String[] parts = line.split(";");
                int id = Integer.parseInt(parts[0]);
                String nome = parts[1];
                TipoRegola tipo = TipoRegola.valueOf(parts[2]);
                ScopeRegola scope = ScopeRegola.valueOf(parts[3]);
                int idRif = parts[4].equals("*") ? -1 : Integer.parseInt(parts[4]);
                String valore = parts[5];
                LocalDate dataInizio = LocalDate.parse(parts[6]);
                LocalDate dataFine = LocalDate.parse(parts[7]);
                String descrizione = parts[8];

                Regola r = new Regola(id, nome, tipo, scope, idRif, valore, dataInizio, dataFine, descrizione);
                result.add(r);
            }
        } catch (IOException e) {
            System.err.println("Errore lettura file regole: " + e.getMessage());
        }
        return result;
    }

    public boolean validaCoerenzaRegola() {
        List<Regola> tutte = mostraTutteLeRegole();

        for (Regola r : tutte) {
            // Ignora la regola stessa se stiamo modificando
            if (r.getId() == this.id && this.id != -1) continue;

            // Controllo solo se TIPO + SCOPE + ID_RIFERIMENTO coincidono
            if (r.getTipo() == this.tipo &&
                    r.getScope() == this.scope &&
                    r.getIdRiferimento() == this.idRiferimento) {

                // Controllo sovrapposizione date
                boolean dateSiSovrappongono =
                        !this.dataFine.isBefore(r.getDataInizio()) &&
                                !this.dataInizio.isAfter(r.getDataFine());

                if (this.tipo == TipoRegola.BLOCCO_GIORNI) {
                    if (dateSiSovrappongono) {
                        // Confronta i giorni bloccati
                        Set<String> giorniCorrenti = Arrays.stream(this.getValore().split(","))
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());

                        Set<String> giorniEsistenti = Arrays.stream(r.getValore().split(","))
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());

                        // Se almeno un giorno coincide → conflitto
                        for (String g : giorniCorrenti) {
                            if (giorniEsistenti.contains(g)) {
                                return false; // conflitto
                            }
                        }
                    }
                } else {
                    // Per gli altri tipi di regola basta il controllo sulle date
                    if (dateSiSovrappongono) {
                        return false; // conflitto
                    }
                }
            }
        }

        return true; // nessun conflitto
    }

    public boolean registraRegola() {
        // assegna ID solo se è nuova regola
        if (this.id == -1) {
            inizializzaNextIdDaFile();
            this.id = nextId++;
        }

        String idRifStr = (idRiferimento == -1) ? "*" : String.valueOf(idRiferimento);
        String riga = String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s",
                id, nome, tipo, scope, idRifStr, valore,
                dataInizio, dataFine, descrizione);

        try {
            File file = new File(FILE_NAME);

            if (file.exists()) {
                // modalità aggiornamento: sostituisci la riga con lo stesso ID
                List<String> righe = Files.readAllLines(file.toPath());

                // mantieni l'intestazione
                String header = righe.isEmpty()
                        ? "// ID;NOME;TIPO;SCOPETIPO;ID_RIFERIMENTO;VALORE;DATA_INIZIO;DATA_FINE;DESCRIZIONE"
                        : righe.get(0);

                List<String> nuoveRighe = new ArrayList<>();
                nuoveRighe.add(header);

                boolean sostituita = false;
                for (int i = 1; i < righe.size(); i++) { // parto da 1 per saltare header
                    String r = righe.get(i);
                    if (r.startsWith(this.id + ";")) {
                        nuoveRighe.add(riga); // sostituisci regola con stesso ID
                        sostituita = true;
                    } else {
                        nuoveRighe.add(r);
                    }
                }

                if (!sostituita) {
                    nuoveRighe.add(riga); // se non trovata, aggiungi in fondo
                }

                Files.write(file.toPath(), nuoveRighe, StandardOpenOption.TRUNCATE_EXISTING);

            } else {
                // modalità inserimento: file non esiste ancora → crealo con intestazione e regola
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("// ID;NOME;TIPO;SCOPETIPO;ID_RIFERIMENTO;VALORE;DATA_INIZIO;DATA_FINE;DESCRIZIONE");
                    writer.newLine();
                    writer.write(riga);
                }
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminaRegola() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return false;

        try {
            List<String> righe = Files.readAllLines(file.toPath());
            List<String> nuoveRighe = new ArrayList<>();

            for (String riga : righe) {
                if (riga.trim().isEmpty() || riga.startsWith("//")) {
                    nuoveRighe.add(riga); // mantieni commento/intestazione
                    continue;
                }
                String[] campi = riga.split(";");
                int idRiga = Integer.parseInt(campi[0]);
                if (idRiga != this.id) { // tieni tutte tranne quella da eliminare
                    nuoveRighe.add(riga);
                }
            }

            Files.write(file.toPath(), nuoveRighe, StandardOpenOption.TRUNCATE_EXISTING);
            return true;

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void inizializzaNextIdDaFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            int maxId = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;
                String[] campi = line.split(";");
                int id = Integer.parseInt(campi[0]);
                if (id > maxId) maxId = id;
            }
            nextId = maxId + 1;
        } catch (IOException | NumberFormatException e) {
            nextId = 1;
        }
    }

    public static boolean verificaPrenotabilità(Prenotazione p) {
        List<Regola> regole = mostraRegole(
                p.getSede().getIdSede(),
                p.getUtente().getIdUtente(),
                p.getData()
        );

        for (Regola r : regole) {
            p.applicaRegola(r);
        }
        return true;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) { this.nome = nome; }

    public int getId() {
        return id;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public TipoRegola getTipo() {
        return tipo;
    }

    public ScopeRegola getScope() {
        return scope;
    }

    public int getIdRiferimento() {
        return idRiferimento;
    }

    public String getValore() {
        return valore;
    }

    public void setValore(String valore) { this.valore = valore; }

    public String getDescrizione() {
        return descrizione;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    @Override
    public String toString() {
        return "Regola{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", tipo=" + tipo +
                ", scope=" + scope +
                ", idRiferimento=" + idRiferimento +
                ", valore='" + valore + '\'' +
                ", dataInizio=" + dataInizio +
                ", dataFine=" + dataFine +
                ", descrizione='" + descrizione + '\'' +
                '}';
    }
}
