package com.agrichain.models;

public class VegetableBatch extends CropBatch {
    private double humidityLevel;

    public VegetableBatch(String batchId, String cropName, String farmerName,
                          double quantityKg, double baseMspPerKg, int remainingShelfLifeDays,
                          double humidityLevel) {
        super(batchId, cropName, farmerName, quantityKg, baseMspPerKg, remainingShelfLifeDays);
        setHumidityLevel(humidityLevel);
    }

    public double getHumidityLevel() {
        return humidityLevel;
    }

    public void setHumidityLevel(double humidityLevel) {
        if (humidityLevel < 0.0 || humidityLevel > 100.0
                || Double.isNaN(humidityLevel) || Double.isInfinite(humidityLevel)) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100 percent.");
        }
        this.humidityLevel = humidityLevel;
    }

    @Override
    public double getMaxSafeTemp() {
        return 8.0;
    }

    // [OOP CONCEPT: RUNTIME POLYMORPHISM - Vegetable-specific storage decay]
    @Override
    public void simulateStorageDay(double currentTemp) {
        if (currentTemp > getMaxSafeTemp()) {
            reduceShelfLife(3);
        } else {
            reduceShelfLife(1);
        }
    }
}