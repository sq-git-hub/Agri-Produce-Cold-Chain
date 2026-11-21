package com.agrichain.contracts;

public interface Perishable {
    void simulateStorageDay(double currentTemp);

    boolean isSpoiled();
}