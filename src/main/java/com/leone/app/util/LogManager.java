package com.leone.app.util;

import java.io.IOException;
import java.util.logging.*;

public class LogManager {
    private static final Logger logger = Logger.getLogger("PrenotazioniLogger");

    static {
        try {
            FileHandler fileHandler = new FileHandler("prenotazioni_scadute.log", true);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    if (record.getMessage().equals("\n")) {
                        return "\n";
                    }
                    return "[" + record.getLevel() + "] " + record.getMessage() + "\n";
                }
            });
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
