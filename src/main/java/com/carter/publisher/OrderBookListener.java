package com.carter.publisher;

public interface OrderBookListener {

    void onOrderUpdate(long orderId, int executedQty, int remainingQty, byte status);

    void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity);

}
