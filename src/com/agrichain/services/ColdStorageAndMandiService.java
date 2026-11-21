package com.agrichain.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.agrichain.events.AgriChainEvent;
import com.agrichain.events.AgriChainEventListener;
import com.agrichain.logging.ApplicationLogger;
import com.agrichain.exceptions.BatchSpoiledException;
import com.agrichain.exceptions.BelowMSPBidException;
import com.agrichain.exceptions.ColdChainBreachException;
import com.agrichain.models.CropBatch;
import com.agrichain.models.GrainBatch;
import com.agrichain.models.VegetableBatch;

public class ColdStorageAndMandiService {
    private final ArrayList<CropBatch> batches;
    private final Map<String, CropBatch> batchIndex;
    private final List<AgriChainEventListener> listeners;
    private final ApplicationLogger logger;
    private double vaultTemperature = 6.0;

    public ColdStorageAndMandiService() {
        this(new ApplicationLogger());
    }

    public ColdStorageAndMandiService(ApplicationLogger logger) {
        batches = new ArrayList<CropBatch>();
        batchIndex = new LinkedHashMap<String, CropBatch>();
        listeners = new ArrayList<AgriChainEventListener>();
        this.logger = logger;
    }

    public double getVaultTemperature() {
        return vaultTemperature;
    }

    public ArrayList<CropBatch> getBatches() {
        return batches;
    }

    public List<CropBatch> snapshotBatches() {
        return new ArrayList<CropBatch>(batches);
    }

    public void addEventListener(AgriChainEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void addBatch(CropBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch cannot be null.");
        }
        if (findBatchOrNull(batch.getBatchId()) != null) {
            throw new IllegalArgumentException("A batch with that ID already exists.");
        }
        batches.add(batch);
        batchIndex.put(batch.getBatchId().toUpperCase(), batch);
        publishEvent("BATCH_REGISTERED", "Batch " + batch.getBatchId() + " registered.");
    }

    public void replaceBatches(List<CropBatch> loadedBatches) {
        batches.clear();
        batchIndex.clear();
        for (CropBatch batch : loadedBatches) {
            if (findBatchOrNull(batch.getBatchId()) != null) {
                throw new IllegalArgumentException("Duplicate batch ID in saved data: "
                        + batch.getBatchId());
            }
            batches.add(batch);
            batchIndex.put(batch.getBatchId().toUpperCase(), batch);
        }
    }

    public void loadSampleData() {
        batches.clear();
        batchIndex.clear();
        addBatch(new VegetableBatch("TOM-101", "Tomatoes", "Asha Patel",
                250.0, 18.0, 5, 88.0));
        addBatch(new VegetableBatch("SPI-102", "Spinach", "Ravi Kumar",
                120.0, 22.0, 3, 92.0));
        addBatch(new GrainBatch("RICE-201", "Basmati Rice", "Meera Singh",
                1000.0, 42.0, 30, 12.5));
        publishEvent("SAMPLES_LOADED", "Three sample batches loaded.");
    }

    // [OOP CONCEPT: RUNTIME POLYMORPHISM - Dispatches decay behavior by batch subtype]
    public void updateVaultTemperature(double newTemp) throws ColdChainBreachException {
        if (Double.isNaN(newTemp) || Double.isInfinite(newTemp)) {
            throw new IllegalArgumentException("Temperature must be a finite number.");
        }

        vaultTemperature = newTemp;
        boolean breachDetected = false;
        String breachedBatchIds = "";

        for (int i = 0; i < batches.size(); i++) {
            CropBatch batch = batches.get(i);
            batch.simulateStorageDay(newTemp);
            if (newTemp > batch.getMaxSafeTemp()) {
                breachDetected = true;
                if (!breachedBatchIds.isEmpty()) {
                    breachedBatchIds += ", ";
                }
                breachedBatchIds += batch.getBatchId();
            }
        }

        if (breachDetected) {
            String message = "Temperature exceeded the safe limit for batch(es): "
                    + breachedBatchIds + ". Shelf-life decay was applied.";
            logger.error(message);
            publishEvent("COLD_CHAIN_BREACH", message);
            throw new ColdChainBreachException(message);
        }
        logger.info("Vault temperature set to " + newTemp + " C.");
        publishEvent("TEMPERATURE_UPDATED", "Vault temperature set to " + newTemp + " C.");
    }

    // [OOP CONCEPT: COMPILE-TIME POLYMORPHISM - Standard bid overload]
    public void placeBid(String batchId, String traderName, double bidPerKg)
            throws BelowMSPBidException {
        processBid(batchId, traderName, bidPerKg);
    }

    // [OOP CONCEPT: COMPILE-TIME POLYMORPHISM - Bulk bid overload]
    public void placeBid(String batchId, String traderName, double bidPerKg,
                        double bulkDiscountBonus) throws BelowMSPBidException {
        if (bulkDiscountBonus < 0.0 || Double.isNaN(bulkDiscountBonus)
                || Double.isInfinite(bulkDiscountBonus)) {
            throw new IllegalArgumentException("Bulk bid bonus must be finite and non-negative.");
        }
        processBid(batchId, traderName, bidPerKg + bulkDiscountBonus);
    }

    private void processBid(String batchId, String traderName, double effectiveBidPerKg)
            throws BelowMSPBidException {
        CropBatch batch = findBatch(batchId);
        if (traderName == null || traderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Trader name cannot be blank.");
        }
        if (batch.isSpoiled()) {
            throw new BatchSpoiledException("Batch " + batchId + " has spoiled and cannot be bid on.");
        }
        if (!batch.isValidBid(effectiveBidPerKg)) {
            throw new BelowMSPBidException("Effective bid must be at least the MSP of "
                    + batch.getMinimumSupportPrice() + " per kg.");
        }
        if (effectiveBidPerKg <= batch.getHighestBidPerKg()) {
            throw new IllegalArgumentException("Bid must exceed the current highest bid of "
                    + batch.getHighestBidPerKg() + " per kg.");
        }

        batch.setHighestBidPerKg(effectiveBidPerKg);
        batch.setHighestBidderName(traderName);
        String message = "Bid accepted for batch " + batchId + " by "
                + traderName + " at " + effectiveBidPerKg + " per kg.";
        logger.info(message);
        publishEvent("BID_ACCEPTED", message);
    }

    private CropBatch findBatch(String batchId) {
        CropBatch batch = findBatchOrNull(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("No batch found with ID " + batchId + ".");
        }
        return batch;
    }

    private CropBatch findBatchOrNull(String batchId) {
        if (batchId == null) {
            return null;
        }
        return batchIndex.get(batchId.trim().toUpperCase());
    }

    private void publishEvent(String eventType, String message) {
        AgriChainEvent event = new AgriChainEvent(eventType, message);
        for (AgriChainEventListener listener : listeners) {
            listener.handleEvent(event);
        }
        logger.logEvent(event);
    }
}