package com.leone.app.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Regola {
    private final int id;
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
        try (BufferedReader reader = new BufferedReader(new FileReader("Regole.txt"))) {
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

    public TipoRegola getTipo() {
        return tipo;
    }

    public String getValore() {
        return valore;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }
}
