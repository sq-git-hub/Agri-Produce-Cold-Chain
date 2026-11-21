package com.agrichain.contracts;

public interface TradeableCommodity {
    double getMinimumSupportPrice();

    boolean isValidBid(double bidAmount);
}