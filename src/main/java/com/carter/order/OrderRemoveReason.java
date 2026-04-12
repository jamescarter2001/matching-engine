package com.carter.order;

public class OrderRemoveReason {

    public static final byte FILLED = 1;
    public static final byte CANCELLED = 2;

    public static boolean isCancelled(byte reason) {
        return reason == CANCELLED;
    }

    public static boolean isFilled(byte reason) {
        return reason == FILLED;
    }

}
