package com.carter.event.processor;

import com.carter.event.OrderEvent;
import com.carter.order.OrderStatus;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderEventProcessor {

    private final Disruptor<OrderEvent> disruptor;

    public OrderEventProcessor() {
        this.disruptor = new Disruptor<>(
                OrderEvent::new,
                1024,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE,
                new BusySpinWaitStrategy()
        );
        disruptor.handleEventsWith(this::onEvent);
    }

    public void start() {
        disruptor.start();
    }

    public void publish(long orderId, byte side, int executedQty, int remainingQty, byte status) {
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setSide(side);
            event.setOrderId(orderId);
            event.setExecutedQty(executedQty);
            event.setRemainingQty(remainingQty);
            event.setStatus(status);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    private void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        log.debug("OrderEvent received: orderId={}, orderSide={}, executedQty={}, remainingQty={}, status={}",
                event.getOrderId(),
                event.getSide(),
                event.getExecutedQty(),
                event.getRemainingQty(),
                OrderStatus.toString(event.getStatus()));
    }

}
