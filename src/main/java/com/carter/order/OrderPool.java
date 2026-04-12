package com.carter.order;

public class OrderPool {

    public static final int MAX_ORDERS = 1 << 10;
    private static final int NULL = -1;

    private final int[] freeList;
    private int nextFree = 0;

    private final long[] orderId;
    private final byte[] side;
    private final int[] price;
    private final int[] qty;
    private final int[] remainingQty;
    private final int[] prevSlot;
    private final int[] nextSlot;

    public OrderPool() {
        this(1024);
    }

    public OrderPool(int poolSize) {
        freeList = new int[poolSize];
        orderId = new long[poolSize];
        side = new byte[poolSize];
        price = new int[poolSize];
        qty = new int[poolSize];
        remainingQty = new int[poolSize];
        prevSlot = new int[MAX_ORDERS];
        nextSlot = new int[MAX_ORDERS];

        init();
    }

    public int acquire() {
        if (nextFree == MAX_ORDERS) {
            throw new IllegalStateException("Pool exhausted");
        }
        return freeList[nextFree++];
    }

    public void release(int slot) {
        if (nextFree == 0) {
            throw new IllegalStateException("Attempt to release slot when pool is full");
        }
        freeList[--nextFree] = slot;
    }

    public long getOrderId(int slot) {
        return orderId[slot];
    }

    public void setOrderId(int slot, long value) {
        orderId[slot] = value;
    }

    public byte getSide(int slot) {
        return side[slot];
    }

    public void setSide(int slot, byte value) {
        side[slot] = value;
    }

    public int getPrice(int slot) {
        return price[slot];
    }

    public void setPrice(int slot, int value) {
        price[slot] = value;
    }

    public int getQty(int slot) {
        return qty[slot];
    }

    public void setQty(int slot, int value) {
        qty[slot] = value;
    }

    public int getRemainingQty(int slot) {
        return remainingQty[slot];
    }

    public void setRemainingQty(int slot, int value) {
        remainingQty[slot] = value;
    }

    public int getPrevSlot(int slot) {
        return prevSlot[slot];
    }

    public void setPrevSlot(int slot, int value) {
        prevSlot[slot] = value;
    }

    public int getNextSlot(int slot) {
        return nextSlot[slot];
    }

    public void setNextSlot(int slot, int value) {
        nextSlot[slot] = value;
    }

    void clear() {
        init();
    }

    private void init() {
        for (int i = 0; i < MAX_ORDERS; i++) {
            freeList[i] = i;
            clearSlot(i);
        }
        nextFree = 0;
    }

    private void clearSlot(int slot) {
        orderId[slot] = 0;
        side[slot] = 0;
        price[slot] = 0;
        remainingQty[slot] = 0;
        qty[slot] = 0;
        prevSlot[slot] = NULL;
        nextSlot[slot] = NULL;
    }

}
