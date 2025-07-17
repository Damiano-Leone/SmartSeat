package com.leone.app.domain;

public class Postazione {
    private final int idPostazione;
    private String codice;
    private String posizione;
    private boolean vicinanzaAFinestra;
    private final Area area;
    private final Dotazione dotazione;

    public Postazione(int idPostazione, String codice,  String posizione, boolean vicinanzaAFinestra, Area area, Dotazione dotazione) {
        this.idPostazione = idPostazione;
        this.codice = codice;
        this.posizione = posizione;
        this.vicinanzaAFinestra = vicinanzaAFinestra;
        this.area = area;
        this.dotazione = dotazione;
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
}
