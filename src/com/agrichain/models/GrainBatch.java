package com.agrichain.models;

public class GrainBatch extends CropBatch {
    private double moisturePercent;

    public GrainBatch(String batchId, String cropName, String farmerName,
                      double quantityKg, double baseMspPerKg, int remainingShelfLifeDays,
                      double moisturePercent) {
        super(batchId, cropName, farmerName, quantityKg, baseMspPerKg, remainingShelfLifeDays);
        setMoisturePercent(moisturePercent);
    }

    public double getMoisturePercent() {
        return moisturePercent;
    }

    public void setMoisturePercent(double moisturePercent) {
        if (moisturePercent < 0.0 || moisturePercent > 100.0
                || Double.isNaN(moisturePercent) || Double.isInfinite(moisturePercent)) {
            throw new IllegalArgumentException("Moisture must be between 0 and 100 percent.");
        }
        this.moisturePercent = moisturePercent;
    }

    @Override
    public double getMaxSafeTemp() {
        return 25.0;
    }

    // [OOP CONCEPT: RUNTIME POLYMORPHISM - Grain-specific storage decay]
    @Override
    public void simulateStorageDay(double currentTemp) {
        if (currentTemp > getMaxSafeTemp() || moisturePercent > 14.0) {
            reduceShelfLife(1);
        }
    }
}