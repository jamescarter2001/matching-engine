package com.carter.order;

public class OrderStatus {

    public static final byte UNKNOWN = 0;
    public static final byte NEW = 1;
    public static final byte PARTIALLY_FILLED = 2;
    public static final byte FILLED = 3;
    public static final byte CANCELLED = 4;
    public static final byte REJECTED = 5;

     private static final String[] statusStrs = {
            "UNKNOWN",
            "NEW",
            "PARTIALLY_FILLED",
            "FILLED",
            "CANCELLED",
            "REJECTED"
    };

    public static String toString(byte status) {
        return statusStrs[status];
    }
}
