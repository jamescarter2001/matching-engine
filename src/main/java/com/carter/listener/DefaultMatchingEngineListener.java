package com.carter.listener;

import com.carter.event.processor.OrderEventProcessor;
import com.carter.event.processor.TradeEventProcessor;
import com.carter.order.OrderSide;
import com.carter.order.OrderStatus;

public class DefaultMatchingEngineListener implements MatchingEngineListener {

    private final OrderEventProcessor orderEventProcessor = new OrderEventProcessor();
    private final TradeEventProcessor tradeEventProcessor = new TradeEventProcessor();

    public DefaultMatchingEngineListener() {
        orderEventProcessor.start();
        tradeEventProcessor.start();
    }

    @Override
    public void onOrderUpdate(long orderId, OrderSide side, int executedQty, int remainingQty, OrderStatus status) {
        orderEventProcessor.publish(orderId, side, executedQty, remainingQty, status);
    }

    @Override
    public void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity) {
        tradeEventProcessor.publish(aggressorOrderId, restingOrderId, price, quantity);
    }
}
