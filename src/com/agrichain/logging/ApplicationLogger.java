package com.agrichain.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.agrichain.events.AgriChainEvent;

public class ApplicationLogger {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logFile;

    public ApplicationLogger() {
        this(Paths.get("logs", "agrichain.log"));
    }

    public ApplicationLogger(Path logFile) {
        this.logFile = logFile;
    }

    public synchronized void log(String level, String message) {
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write("[" + LocalDateTime.now().format(FORMATTER) + "] "
                        + level + " " + message);
                writer.newLine();
            }
        } catch (IOException exception) {
            System.err.println("Unable to write application log: " + exception.getMessage());
        }
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void logEvent(AgriChainEvent event) {
        log("EVENT", event.toString());
    }
}
