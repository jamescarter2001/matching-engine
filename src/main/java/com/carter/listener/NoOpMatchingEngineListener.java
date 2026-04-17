package com.carter.listener;

import com.carter.order.OrderSide;
import com.carter.order.OrderStatus;

public class NoOpMatchingEngineListener implements MatchingEngineListener {

    @Override
    public void onOrderUpdate(long orderId, OrderSide side, int executedQty, int remainingQty, OrderStatus status) {
        // No-op
    }

    @Override
    public void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity) {
        // No-op
    }

}
