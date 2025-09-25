package com.leone.app;

import com.leone.app.domain.SmartSeat;
import com.leone.app.domain.Utente;
import com.leone.app.util.PrenotazioneScheduler;

import java.util.Scanner;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        SmartSeat smartSeat = SmartSeat.getInstance();
        Scanner scanner = new Scanner(System.in);

        // 🔄 Avvio dello scheduler
        PrenotazioneScheduler prenotazioneScheduler = new PrenotazioneScheduler();
        prenotazioneScheduler.avviaControlloPrenotazioni();

        boolean continua = true;
        while (continua) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Prenota postazione");
            System.out.println("2. Check-in postazione");
            System.out.println("3. Configurazione regole prenotazione");
            System.out.println("0. Esci");
            System.out.print("Scegli un’opzione: ");

            String scelta = scanner.nextLine();

            switch (scelta) {
                case "1" -> {
                    try {
                        Utente utentePrenotazione = smartSeat.selezionaUtenteDaConsole("utente");
                        smartSeat.avviaPrenotazione(utentePrenotazione.getIdUtente());
                        smartSeat.selezionaParametriDaConsole();
                        smartSeat.selezionaFiltriDaConsole();
                        if (smartSeat.selezionaPostazioneDaConsole())
                            smartSeat.confermaPrenotazioneDaConsole();
                    } catch (IllegalStateException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "2" -> {
                    try {
                        smartSeat.effettuaCheckIn();
                    } catch (IllegalStateException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "3" -> {
                    try {
                        Utente admin = smartSeat.selezionaUtenteDaConsole("admin");
                        if (!admin.getRuolo().equalsIgnoreCase("ADMIN")) {
                            break;
                        }
                        smartSeat.gestisciRegoleDaConsole();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "0" -> {
                    System.out.println("Arrivederci!");
                    continua = false;
                    prenotazioneScheduler.stopScheduler(); // ferma lo scheduler alla chiusura
                }
                default -> System.out.println("Opzione non valida.");
            }
        }
    }
}
