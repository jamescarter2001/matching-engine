package com.carter.order;

public class OrderSide {

    public static final byte BUY = 1;
    public static final byte SELL = 2;

    public static boolean isBuy(byte value) {
        return value == BUY;
    }
    public static boolean isSell(byte value) {
        return value == SELL;
    }

}
