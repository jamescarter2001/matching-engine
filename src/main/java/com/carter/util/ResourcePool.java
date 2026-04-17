package com.carter.util;

import com.google.common.base.Preconditions;

import java.util.function.Supplier;

public class ResourcePool<T> {

    private final T[] pool;
    private final int size;

    private int nextFree;

    public ResourcePool(Supplier<T> supplier) {
        this(supplier, 1024);
    }

    public ResourcePool(Supplier<T> supplier, int size) {
        Preconditions.checkArgument(MathX.isPowerOfTwo(size), "Pool size must be a power of two.");
        this.size = size;
        this.pool = (T[]) new Object[size];
        for (int i = 0; i < size; i++) {
            pool[i] = supplier.get();
        }
        nextFree = size-1;
    }

    public T acquire() {
        if (nextFree == -1) {
            throw new IllegalStateException("Pool exhausted.");
        }
        return pool[nextFree--];
    }

    public void release(T obj) {
        if (nextFree == size-1) {
            throw new IllegalStateException("Pool is full - is there a double free?");
        }
        pool[++nextFree] = obj;

    }

}
