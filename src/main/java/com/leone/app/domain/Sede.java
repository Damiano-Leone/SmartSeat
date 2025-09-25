package com.leone.app.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sede {
    private final int idSede;
    private String nome;
    private String indirizzo;
    private Map<Integer, Area> elencoAree;

    private static String FILE_NAME = "Sedi.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Sede(int idSede, String nome, String indirizzo) {
        this.idSede = idSede;
        this.nome = nome;
        this.indirizzo = indirizzo;
        this.elencoAree = new HashMap<>();
    }

    public int getIdSede() {
        return idSede;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public void addArea(Area area) {
        elencoAree.put(area.getIdArea(), area);
    }

    public Map<Integer, Area> getElencoAree() {
        return elencoAree;
    }

    public List<Postazione> getPostazioni() {
        List<Postazione> postazioni = new ArrayList<>();
        for (Area area : getElencoAree().values()) {
            postazioni.addAll(area.getElencoPostazioni().values());
        }
        return postazioni;
    }

    @Override
    public String toString() {
        return "Sede{" +
                "idSede=" + idSede +
                ", nome='" + nome + '\'' +
                ", indirizzo='" + indirizzo + '\'' +
                '}';
    }

    public static Sede getById(int id) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;

                // Split considerando eventuali virgolette
                String[] campi = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                int idSede = Integer.parseInt(campi[0].trim());
                if (idSede == id) {
                    // Rimuovo eventuali virgolette
                    String nome = campi[1].trim().replaceAll("^\"|\"$", "");
                    String indirizzo = campi[2].trim().replaceAll("^\"|\"$", "");

                    return new Sede(idSede, nome, indirizzo);
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

}
