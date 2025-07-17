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

        int idUtente = smartSeat.selezionaUtenteDaConsole();

        boolean continua = true;
        while (continua) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Prenota postazione");
            System.out.println("0. Esci");
            System.out.print("Scegli un’opzione: ");

            String scelta = scanner.nextLine();

            switch (scelta) {
                case "1" -> {
                    try {
                        smartSeat.avviaPrenotazione(idUtente);
                        smartSeat.selezionaParametriDaConsole();
                        smartSeat.selezionaFiltriDaConsole();
                        smartSeat.selezionaPostazioneDaConsole();
                        smartSeat.confermaPrenotazione();
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
