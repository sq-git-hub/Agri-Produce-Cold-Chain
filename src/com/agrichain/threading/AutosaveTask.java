package com.agrichain.threading;

import java.io.IOException;
import java.util.List;

import com.agrichain.logging.ApplicationLogger;
import com.agrichain.models.CropBatch;
import com.agrichain.persistence.BatchFileStore;

public class AutosaveTask implements Runnable {
    private final BatchFileStore store;
    private final ApplicationLogger logger;
    private final AutosaveSource source;

    @FunctionalInterface
    public interface AutosaveSource {
        List<CropBatch> snapshot();
    }

    public AutosaveTask(BatchFileStore store, ApplicationLogger logger,
                        AutosaveSource source) {
        this.store = store;
        this.logger = logger;
        this.source = source;
    }

    @Override
    public void run() {
        try {
            store.save(source.snapshot());
            logger.info("Background autosave completed.");
        } catch (IOException exception) {
            logger.error("Background autosave failed: " + exception.getMessage());
        }
    }
}
