package com.carter.order;

public enum OrderSide {
    UNKNOWN,
    BUY,
    SELL;

    public boolean isBuy() {
        return this == BUY;
    }
}
