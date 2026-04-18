package com.carter.order;

import uk.co.real_logic.sbe.benchmarks.fix.Side;

public class OrderSide {

    public static final byte UNKNOWN = 0;
    public static final byte BUY = 1;
    public static final byte SELL = 2;

    public static boolean isBuy(byte side) {
        return side == BUY;
    }

    public static byte fromSide(Side side) {
        return switch (side) {
            case BUY -> BUY;
            case SELL -> SELL;
            default -> UNKNOWN;
        };
    }
}
