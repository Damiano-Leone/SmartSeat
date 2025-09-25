package com.leone.app.domain;

import java.io.*;
import java.time.LocalDateTime;

public class CheckIn {
    private static int nextId = 1;
    private int idCheckIn;
    private final int idUtente;
    private final int idPrenotazione;
    private final int idPostazione;
    private LocalDateTime timestamp;
    private final String metodo; // es. "QR", "Manuale"
    private static final Object FILE_LOCK = new Object();

    private static String FILE_NAME = "CheckIn.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public CheckIn(int idUtente, int idPrenotazione, int idPostazione, String metodo) {
        this.idCheckIn = -1;
        this.idUtente = idUtente;
        this.idPrenotazione = idPrenotazione;
        this.idPostazione = idPostazione;
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


    /** Registra il check-in su file */
    public boolean registraCheckIn() {
        inizializzaNextIdDaFile();
        this.idCheckIn = nextId++;
        this.timestamp = LocalDateTime.now();
        String riga = String.format("%d,%d,%d,%d,%s,%s",
                idCheckIn,
                idUtente,
                idPrenotazione,
                idPostazione,
                timestamp,
                metodo
        );
        synchronized (FILE_LOCK) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
                writer.newLine();
                writer.write(riga);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean checkInGiaEffettuato(int idPrenotazione) {
        synchronized (FILE_LOCK) {
            try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // salta righe vuote o commenti (es. intestazione // ...)
                    if (line.isEmpty() || line.startsWith("//")) continue;
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
        }
        return false;
    }
}
