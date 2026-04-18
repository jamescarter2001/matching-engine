package com.carter.listener;

import com.carter.order.OrderSide;
import com.carter.order.OrderStatus;

public interface MatchingEngineListener {

    void onOrderUpdate(long orderId, byte side, int executedQty, int remainingQty, byte status);

    void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity);

}
