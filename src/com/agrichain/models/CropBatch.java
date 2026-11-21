package com.agrichain.models;

import com.agrichain.contracts.Perishable;
import com.agrichain.contracts.TradeableCommodity;

public abstract class CropBatch implements Perishable, TradeableCommodity {
    private String batchId;
    private String cropName;
    private String farmerName;
    private double quantityKg;
    private double baseMspPerKg;
    private int remainingShelfLifeDays;
    private double highestBidPerKg;
    private String highestBidderName;

    public CropBatch(String batchId, String cropName, String farmerName,
                     double quantityKg, double baseMspPerKg, int remainingShelfLifeDays) {
        setBatchId(batchId);
        setCropName(cropName);
        setFarmerName(farmerName);
        setQuantityKg(quantityKg);
        setBaseMspPerKg(baseMspPerKg);
        setRemainingShelfLifeDays(remainingShelfLifeDays);
        setHighestBidPerKg(0.0);
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = requireText(batchId, "Batch ID");
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = requireText(cropName, "Crop name");
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = requireText(farmerName, "Farmer name");
    }

    public double getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(double quantityKg) {
        if (quantityKg <= 0.0 || Double.isNaN(quantityKg) || Double.isInfinite(quantityKg)) {
            throw new IllegalArgumentException("Quantity must be a finite value above zero.");
        }
        this.quantityKg = quantityKg;
    }

    public double getBaseMspPerKg() {
        return baseMspPerKg;
    }

    public void setBaseMspPerKg(double baseMspPerKg) {
        if (baseMspPerKg <= 0.0 || Double.isNaN(baseMspPerKg) || Double.isInfinite(baseMspPerKg)) {
            throw new IllegalArgumentException("MSP must be a finite value above zero.");
        }
        this.baseMspPerKg = baseMspPerKg;
    }

    public int getRemainingShelfLifeDays() {
        return remainingShelfLifeDays;
    }

    public void setRemainingShelfLifeDays(int remainingShelfLifeDays) {
        if (remainingShelfLifeDays < 0) {
            throw new IllegalArgumentException("Remaining shelf life cannot be negative.");
        }
        this.remainingShelfLifeDays = remainingShelfLifeDays;
    }

    public double getHighestBidPerKg() {
        return highestBidPerKg;
    }

    public void setHighestBidPerKg(double highestBidPerKg) {
        if (highestBidPerKg < 0.0 || Double.isNaN(highestBidPerKg)
                || Double.isInfinite(highestBidPerKg)) {
            throw new IllegalArgumentException("Highest bid must be a finite, non-negative value.");
        }
        this.highestBidPerKg = highestBidPerKg;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public void setHighestBidderName(String highestBidderName) {
        this.highestBidderName = requireText(highestBidderName, "Bidder name");
    }

    @Override
    public double getMinimumSupportPrice() {
        return baseMspPerKg;
    }

    @Override
    public boolean isValidBid(double bidAmount) {
        return !Double.isNaN(bidAmount) && !Double.isInfinite(bidAmount)
                && bidAmount >= getMinimumSupportPrice();
    }

    @Override
    public boolean isSpoiled() {
        return remainingShelfLifeDays == 0;
    }

    public abstract double getMaxSafeTemp();

    protected void reduceShelfLife(int days) {
        remainingShelfLifeDays = Math.max(0, remainingShelfLifeDays - days);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}