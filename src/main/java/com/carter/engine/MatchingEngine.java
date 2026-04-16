package com.carter.engine;

import com.carter.event.OrderEvent;
import com.carter.event.TradeEvent;
import com.carter.event.handler.OrderEventHandler;
import com.carter.event.handler.TradeEventHandler;
import com.carter.event.processor.OrderEventProcessor;
import com.carter.event.processor.TradeEventProcessor;
import com.carter.order.OrderBook;
import com.carter.order.OrderPool;
import com.carter.publisher.OrderBookListener;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;

@Slf4j
public class MatchingEngine {

    private static final int MAX_ORDERS = 1 << 10;

    private final Long2ObjectHashMap<OrderBook> books = new Long2ObjectHashMap<>(16, 0.8f, true);
    private final Long2LongHashMap instrumentIdByOrderId = new Long2LongHashMap(MAX_ORDERS, 0.8f, -1, true);
    private final OrderPool orderPool = new OrderPool(MAX_ORDERS);

    private long nextOrderId = 0;

    public MatchingEngine() {
        TradeEventHandler tradeEventHandler = new TradeEventHandler();
        OrderEventHandler orderEventHandler = new OrderEventHandler();
        this(tradeEventHandler, orderEventHandler);
    }

    public MatchingEngine(EventHandler<TradeEvent> tradeHandler, EventHandler<OrderEvent> orderHandler) {
        TradeEventProcessor tradeEventProcessor = new TradeEventProcessor(tradeHandler);
        OrderEventProcessor orderEventProcessor = new OrderEventProcessor(orderHandler);

        tradeEventProcessor.start();
        orderEventProcessor.start();

        OrderBookListener listener = new OrderBookListener() {
            @Override
            public void onOrderUpdate(long orderId, byte side, int executedQty, int remainingQty, byte status) {
                orderEventProcessor.publish(orderId, side, executedQty, remainingQty, status);
            }

            @Override
            public void onRestingOrderFilled(long orderId) {
                instrumentIdByOrderId.remove(orderId);
            }

            @Override
            public void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity) {
                tradeEventProcessor.publish(aggressorOrderId, restingOrderId, price, quantity);
            }
        };

        init(listener);
    }

    public void init(OrderBookListener listener) {
        books.put(1, new OrderBook(orderPool, listener, 1000, 3000, 1));
        books.put(2, new OrderBook(orderPool, listener, 0, 1000, 1));
    }

    public int getBestBid(long instrumentId) {
        return books.get(instrumentId).getBestBid();
    }

    public int getBestAsk(long instrumentId) {
        return books.get(instrumentId).getBestAsk();
    }

    public long addOrder(long instrumentId, int price, int quantity, byte side) {
        long orderId = nextOrderId++;
        OrderBook book = books.get(instrumentId);
        boolean rested = book.addOrder(orderId, price, quantity, side);
        if (rested) instrumentIdByOrderId.put(orderId, instrumentId);
        return orderId;
    }

    public long cancelOrder(long orderId) {
        long symbolId = instrumentIdByOrderId.remove(orderId);
        OrderBook book = books.get(symbolId);
        book.cancelOrder(orderId);
        return orderId;
    }

    void clear() {
        orderPool.clear();
        instrumentIdByOrderId.clear();
        nextOrderId = 0;
    }

}
