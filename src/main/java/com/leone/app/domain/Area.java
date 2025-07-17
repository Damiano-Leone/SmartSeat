package com.leone.app.domain;

import java.util.HashMap;
import java.util.Map;

public class Area {
    private final int idArea;
    private String nome;
    private String descrizione;
    private final Sede sede;
    private Map<Integer, Postazione> elencoPostazioni;

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
}
