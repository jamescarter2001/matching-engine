package com.carter.event.handler;

import com.carter.event.OrderEvent;
import com.carter.order.OrderStatus;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderEventHandler implements EventHandler<OrderEvent> {
    @Override
    public void onEvent(OrderEvent orderEvent, long sequence, boolean endOfBatch) {
        log.info("OrderEvent received: orderId={}, orderSide={}, executedQty={}, remainingQty={}, status={}",
                orderEvent.getOrderId(),
                orderEvent.getSide(),
                orderEvent.getExecutedQty(),
                orderEvent.getRemainingQty(),
                OrderStatus.toString(orderEvent.getStatus()));
    }
}
