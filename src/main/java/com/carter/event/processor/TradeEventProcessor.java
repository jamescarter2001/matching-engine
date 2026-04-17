package com.carter.event.processor;

import com.carter.event.TradeEvent;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeEventProcessor {

    private final Disruptor<TradeEvent> disruptor;

    public TradeEventProcessor() {
        this.disruptor = new Disruptor<>(
                TradeEvent::new,
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

    public void publish(long aggressorOrderId, long restingOrderId, int price, int quantity) {
        RingBuffer<TradeEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            TradeEvent event = ringBuffer.get(sequence);
            event.setAggressorOrderId(aggressorOrderId);
            event.setRestingOrderId(restingOrderId);
            event.setPrice(price);
            event.setQuantity(quantity);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    private void onEvent(TradeEvent event, long sequence, boolean endOfBatch) {
        log.info("TradeEvent received: aggressorOrderId={}, restingOrderId={}, price={}, quantity={}",
                event.getAggressorOrderId(),
                event.getRestingOrderId(),
                event.getPrice(),
                event.getQuantity());
    }

}
