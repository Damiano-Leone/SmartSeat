package com.leone.app.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Area {
    private final int idArea;
    private String nome;
    private String descrizione;
    private final Sede sede;
    private Map<Integer, Postazione> elencoPostazioni;

    private static String FILE_NAME = "Aree.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Area(int idArea, String nome, String descrizione, Sede sede) {
        this.idArea = idArea;
        this.nome = nome;
        this.descrizione = descrizione;
        this.sede = sede;
        this.elencoPostazioni = new HashMap<>();
    }

    public int getIdArea() {
        return idArea;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Sede getSede() {
        return sede;
    }

    public void addPostazione(Postazione postazione) {
        elencoPostazioni.put(postazione.getIdPostazione(), postazione);
    }

    public Map<Integer, Postazione> getElencoPostazioni() {
        return elencoPostazioni;
    }

    @Override
    public String toString() {
        return "Area{" +
                "idArea=" + idArea +
                ", nome='" + nome + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", sede=" + sede +
                '}';
    }

    public static Area getById(int id) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;

                String[] campi = line.split(",");

                int idArea = Integer.parseInt(campi[0].trim());
                if (idArea == id) {
                    String nome = campi[1].trim();
                    String descrizione = campi[2].trim();
                    int idSede = Integer.parseInt(campi[3].trim());

                    // recupero la sede collegata
                    Sede sede = Sede.getById(idSede);

                    return new Area(idArea, nome, descrizione, sede);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
