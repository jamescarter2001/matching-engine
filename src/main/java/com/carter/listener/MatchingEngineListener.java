package com.carter.listener;

import com.carter.order.OrderSide;
import com.carter.order.OrderStatus;

public interface MatchingEngineListener {

    void onOrderUpdate(long orderId, OrderSide side, int executedQty, int remainingQty, OrderStatus status);

    void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity);

}
