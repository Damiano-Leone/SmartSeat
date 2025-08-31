package com.leone.app.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sede {
    private final int idSede;
    private String nome;
    private String indirizzo;
    private Map<Integer, Area> elencoAree;

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
}
