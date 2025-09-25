package com.leone.app.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Postazione {
    private final int idPostazione;
    private String codice;
    private String posizione;
    private boolean vicinanzaAFinestra;
    private final Area area;
    private final Dotazione dotazione;

    private static String FILE_NAME = "Postazioni.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Postazione(int idPostazione) {
        this.idPostazione = idPostazione;
        this.codice = null;
        this.posizione = null;
        this.vicinanzaAFinestra = false;
        this.area = null;
        this.dotazione = null;
    }

    public Postazione(int idPostazione, String codice,  String posizione, boolean vicinanzaAFinestra, Area area, Dotazione dotazione) {
        this.idPostazione = idPostazione;
        this.codice = codice;
        this.posizione = posizione;
        this.vicinanzaAFinestra = vicinanzaAFinestra;
        this.area = area;
        this.dotazione = dotazione;
    }

    public Postazione(int idPostazione, String codice,  String posizione, boolean vicinanzaAFinestra, Area area) {
        this.idPostazione = idPostazione;
        this.codice = codice;
        this.posizione = posizione;
        this.vicinanzaAFinestra = vicinanzaAFinestra;
        this.area = area;
        this.dotazione = null;
    }

    public int getIdPostazione() {
        return idPostazione;
    }

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getPosizione() {
        return posizione;
    }

    public void setPosizione(String posizione) {
        this.posizione = posizione;
    }

    public boolean isVicinanzaAFinestra() {
        return vicinanzaAFinestra;
    }

    public void setVicinanzaAFinestra(boolean vicinanzaAFinestra) {
        this.vicinanzaAFinestra = vicinanzaAFinestra;
    }

    public Area getArea() {
        return area;
    }

    public Dotazione getDotazione() {
        return dotazione;
    }

    @Override
    public String toString() {
        return "Postazione{" +
                "idPostazione=" + idPostazione +
                ", codice='" + codice + '\'' +
                ", posizione='" + posizione + '\'' +
                ", vicinanzaAFinestra=" + vicinanzaAFinestra +
                ", area=" + area +
                ", dotazione=" + dotazione +
                '}';
    }

    public static Postazione getById(int id) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;

                String[] campi = line.split(",");

                int idPostazione = Integer.parseInt(campi[0].trim());
                if (idPostazione == id) {
                    String codice = campi[1].trim();
                    String posizione = campi[2].trim();
                    boolean vicinanzaAFinestra = Boolean.parseBoolean(campi[3].trim());
                    int idArea = Integer.parseInt(campi[4].trim());
                    int idDotazione = Integer.parseInt(campi[5].trim());
                    Area area = Area.getById(idArea);

                    return new Postazione(idPostazione, codice, posizione, vicinanzaAFinestra, area);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
