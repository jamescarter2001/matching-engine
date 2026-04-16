package com.carter.event.processor;

import com.carter.event.OrderEvent;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

public class OrderEventProcessor {

    private final Disruptor<OrderEvent> disruptor;

    public OrderEventProcessor(EventHandler<OrderEvent> handler) {
        this.disruptor = new Disruptor<>(
                OrderEvent::new,
                1024,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE,
                new BusySpinWaitStrategy()
        );
        disruptor.handleEventsWith(handler);
    }

    public void start() {
        disruptor.start();
    }

    public void publish(long orderId, byte side, int executedQty, int remainingQty, byte status) {
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrderId(orderId);
            event.setExecutedQty(executedQty);
            event.setRemainingQty(remainingQty);
            event.setStatus(status);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
