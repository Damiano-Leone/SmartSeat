package com.leone.app.domain;

public class Dotazione {
    private final int idDotazione;
    private String nome;
    private String descrizione;

    private static String FILE_NAME = "Dotazioni.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Dotazione(int idDotazione, String nome, String descrizione) {
        this.idDotazione = idDotazione;
        this.nome = nome;
        this.descrizione = descrizione;
    }

    public int getIdDotazione() {
        return idDotazione;
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

    @Override
    public String toString() {
        return "Dotazione{" +
                "idDotazione=" + idDotazione +
                ", nome='" + nome + '\'' +
                ", descrizione='" + descrizione + '\'' +
                '}';
    }
}
