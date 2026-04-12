package com.carter.publisher;

public interface TradePublisher {
    void publish(long aggressorOrderId, long restingOrderId, int price, int quantity);
}
