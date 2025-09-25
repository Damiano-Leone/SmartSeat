package com.leone.app.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Utente {
    private final int idUtente;
    private String nome;
    private String cognome;
    private String email;
    private String ruolo;

    private static String FILE_NAME = "Utenti.txt";

    public static void setFileName(String fileName) {
        FILE_NAME = fileName;
    }

    public Utente(int idUtente) {
        this.idUtente = idUtente;
        this.nome = null;
        this.cognome = null;
        this.email = null;
        this.ruolo = null;
    }

    public Utente(int idUtente, String nome, String cognome, String email, String ruolo) {
        this.idUtente = idUtente;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.ruolo = ruolo;
    }

    public int getIdUtente() {
        return idUtente;
    }


    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    @Override
    public String toString() {
        return "Utente{" +
                "idUtente=" + idUtente +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", email='" + email + '\'' +
                ", ruolo='" + ruolo + '\'' +
                '}';
    }

    public static Utente getById(int id) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) continue;
                String[] campi = line.split(",");
                int idUtente = Integer.parseInt(campi[0]);
                if (idUtente == id) {
                    String nome = campi[1];
                    String cognome = campi[2];
                    String email = campi[3];
                    String ruolo = campi[4];
                    return new Utente(idUtente, nome, cognome, email, ruolo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
