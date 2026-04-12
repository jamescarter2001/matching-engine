package com.carter.publisher;

public interface OrderEventPublisher {
    void publish(long orderId, int executedQty, int remainingQty, byte status);
}
