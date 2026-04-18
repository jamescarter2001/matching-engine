package com.carter.order;

public class OrderRemovedReason {

    public static final byte FILLED = 1;
    public static final byte CANCELLED = 2;

    public static boolean isFilled(byte reason) {
        return reason == FILLED;
    }
}
