package com.carter.publisher;

public interface OrderBookListener {

    void onOrderUpdate(long orderId, byte side, int executedQty, int remainingQty, byte status);

    void onOrderRemoved(long orderId);

    void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity);

}
