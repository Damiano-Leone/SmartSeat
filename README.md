# SmartSeat

SmartSeat is an application designed to manage the reservation and usage of workstations in shared environments such as study rooms, corporate open spaces, and coworking areas.  
It allows users to easily book a workstation, check in upon arrival using a QR code, and enables administrators to monitor real-time occupancy to optimize space management.

---

## Configurazione del progetto

Per configurare il progetto seguire i passaggi:

1. Inserire il file `.env` nella directory principale del progetto (in cui è già presente un file `.env` placeholder).  
   - Questo file contiene i parametri per il servizio di invio email, già configurati su un account dedicato con *app password*.  
   - Non è incluso nel repository poiché contiene credenziali necessarie al corretto funzionamento dell’applicazione.

2. Sostituire le email di test `test@example.com` con una email valida alla quale si desidera ricevere le notifiche:  
   - File `Utenti.txt` (tutti gli utenti con `test@example.com`)  
   - File `TestPrenotazioneScheduler.java` (riga 20)  
   - File `TestNotifica.java` (riga 12)  
   - File `TestSmartSeat.java` (riga 15)  

Dopo la sostituzione, l’applicazione invierà correttamente le email di conferma/annullamento delle prenotazioni all’indirizzo specificato.

---

## Contributors

- Damiano Leone
