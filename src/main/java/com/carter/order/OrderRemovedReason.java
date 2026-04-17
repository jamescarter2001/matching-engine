package com.carter.order;

public enum OrderRemovedReason {
    FILLED,
    CANCELLED;

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isFilled() {
        return this == FILLED;
    }
}
