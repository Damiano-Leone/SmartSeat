package com.leone.app.domain;

import java.io.*;
import java.time.LocalDateTime;

public class CheckIn {
    private static int nextId = 1;
    private int idCheckIn;
    private final int idUtente;
    private final int idPrenotazione;
    private final int idPostazione;
    private final LocalDateTime timestamp;
    private final String metodo; // es. "QR", "Manuale"

    public CheckIn(int idUtente, int idPrenotazione, int idPostazione, String metodo) {
        this.idCheckIn = -1;
        this.idUtente = idUtente;
        this.idPrenotazione = idPrenotazione;
        this.idPostazione = idPostazione;
        this.timestamp = LocalDateTime.now();
        this.metodo = metodo;
    }

    public int getIdCheckIn() {
        return idCheckIn;
    }

    public int getIdUtente() {
        return idUtente;
    }

    public int getIdPrenotazione() {
        return idPrenotazione;
    }

    public int getIdPostazione() {
        return idPostazione;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMetodo() {
        return metodo;
    }

    @Override
    public String toString() {
        return "CheckIn{" +
                "idCheckIn=" + idCheckIn +
                ", idUtente=" + idUtente +
                ", idPrenotazione=" + idPrenotazione +
                ", idPostazione=" + idPostazione +
                ", timestamp=" + timestamp +
                ", metodo='" + metodo + '\'' +
                '}';
    }

    public static void inizializzaNextIdDaFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("CheckIn.txt"))) {
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


    /** Registra il check-in su file */
    public boolean registraCheckIn() {
        inizializzaNextIdDaFile();
        this.idCheckIn = nextId++;
        String riga = String.format("%d,%d,%d,%d,%s,%s",
                idCheckIn,
                idUtente,
                idPrenotazione,
                idPostazione,
                timestamp,
                metodo
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("CheckIn.txt", true))) {
            writer.newLine();
            writer.write(riga);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkInGiaEffettuato(int idPrenotazione) {
        try (BufferedReader reader = new BufferedReader(new FileReader("CheckIn.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] campi = line.split(",");
                if (campi.length >= 3) { // idPrenotazione è il terzo campo
                    int idPren = Integer.parseInt(campi[2]);
                    if (idPren == idPrenotazione) {
                        return true; // check-in già presente
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
