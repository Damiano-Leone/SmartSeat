package com.leone.app.util;

import com.leone.app.domain.CheckIn;
import com.leone.app.domain.Notifica;
import com.leone.app.domain.Prenotazione;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PrenotazioneScheduler implements Runnable {
    private ScheduledExecutorService scheduler;
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void run() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("Inizio esecuzione ciclo di controllo prenotazioni: " + timestamp);
        gestisciPrenotazioniScadute();
        logger.info("\n");
    }

    private void gestisciPrenotazioniScadute() {
        List<Prenotazione> tuttePrenotazioni = Prenotazione.mostraTutteLePrenotazioni();
        LocalDateTime adesso = LocalDateTime.now();

        for (Prenotazione p : tuttePrenotazioni) {
            try {
                LocalDate dataPrenotazione = p.getData();
                String fascia = p.getFasciaOraria();
                String inizioFasciaStr = fascia.split("-")[0].trim();

                LocalTime inizioFascia = LocalTime.parse(inizioFasciaStr, DateTimeFormatter.ofPattern("HH:mm"));
                LocalDateTime scadenzaCheckIn = LocalDateTime.of(dataPrenotazione, inizioFascia).plusMinutes(15);

                // Condizione: oltre la scadenza e nessun check-in
                if (adesso.isAfter(scadenzaCheckIn) && !CheckIn.checkInGiaEffettuato(p.getId())) {
                    logger.warning("⚠️ Check-in scaduto: Prenotazione [ID:" + p.getId() +
                            "] | Utente:" + p.getUtente().getNome() + " " + p.getUtente().getCognome() +
                            " | Data:" + p.getData() +
                            " | Orario:" + p.getFasciaOraria() +
                            " | Postazione:" + p.getPostazione().getCodice() + " " + p.getPostazione().getPosizione());

                    if (p.annullaPrenotazione()) {
                        logger.info("✅ Prenotazione ID [" + p.getId() + "] annullata.");
                        try {
                            Notifica.generaNotificaAnnullamento(p, null, true);
                        } catch (Exception e) {
                            logger.severe("❌ Errore nell'invio email a " + p.getUtente().getEmail() +
                                    " | Dettagli: " + e.getMessage());
                        }
                    } else {
                        logger.severe("❌ Errore durante l'annullamento della prenotazione ID " + p.getId());
                    }
                }
            } catch (Exception ex) {
                logger.severe("❌ Errore durante il controllo no-show per prenotazione ID " + p.getId() +
                        " | Dettagli: " + ex.getMessage());
            }
        }
    }

    public void avviaControlloPrenotazioni() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 0, 5, TimeUnit.MINUTES);
    }

    public void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}