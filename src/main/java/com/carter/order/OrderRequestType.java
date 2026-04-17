package com.carter.order;

public class OrderRequestType {

    public static final byte NEW = 1;

    public static boolean isNew(byte requestType) {
        return requestType == NEW;
    }

}
