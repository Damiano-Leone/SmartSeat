package com.leone.app;

import com.leone.app.domain.SmartSeat;

import java.util.Scanner;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        SmartSeat smartSeat = SmartSeat.getInstance();
        Scanner scanner = new Scanner(System.in);

        boolean continua = true;
        while (continua) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Prenota postazione");
            System.out.println("2. Check-in postazione");
            System.out.println("0. Esci");
            System.out.print("Scegli un’opzione: ");

            String scelta = scanner.nextLine();

            switch (scelta) {
                case "1" -> {
                    try {
                        int idUtente = smartSeat.selezionaUtenteDaConsole();
                        smartSeat.avviaPrenotazione(idUtente);
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
                case "0" -> {
                    System.out.println("Arrivederci!");
                    continua = false;
                }
                default -> System.out.println("Opzione non valida.");
            }
        }
    }
}
