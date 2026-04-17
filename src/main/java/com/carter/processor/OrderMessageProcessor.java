package com.carter.processor;

import com.carter.engine.MatchingEngine;
import com.carter.order.OrderRequest;
import com.carter.order.OrderRequestType;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.agrona.DirectBuffer;
import uk.co.real_logic.sbe.benchmarks.fix.NewOrderDecoder;

public class OrderMessageProcessor {

    private final Disruptor<OrderRequest> disruptor = new Disruptor<>(
            OrderRequest::new,
            1024,
            DaemonThreadFactory.INSTANCE,
            ProducerType.SINGLE,
            new BusySpinWaitStrategy()
    );

    private final MatchingEngine matchingEngine = new MatchingEngine();

    private RingBuffer<OrderRequest> ringBuffer;

    private final NewOrderDecoder newOrderDecoder = new NewOrderDecoder();

    public void start() {
        disruptor.handleEventsWith(this::handleEvent);
        ringBuffer = disruptor.start();
    }

    public void processNewOrder(DirectBuffer buffer, int offset, int blockLength, int version) {
        newOrderDecoder.wrap(buffer, offset, blockLength, version);

        long sequence = ringBuffer.next();
        try {
            OrderRequest request = ringBuffer.get(sequence);
            request.setRequestType(OrderRequestType.NEW);
            request.setSide((byte) (newOrderDecoder.sideRaw() - '0'));
            request.setQuantity(newOrderDecoder.orderQty().mantissa());
            request.setInstrumentId(1);
            request.setPrice((int) newOrderDecoder.price().mantissa());
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    private void handleEvent(OrderRequest req, long seq, boolean endOfBatch) {
        switch (req.getRequestType()) {
            case OrderRequestType.NEW -> matchingEngine.addOrder(
                    req.getInstrumentId(),
                    req.getPrice(),
                    req.getQuantity(),
                    req.getSide());
            default -> throw new IllegalStateException("Unsupported request type: " + req.getRequestType());
        }
    }

}
