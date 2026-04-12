package com.carter.engine;

import com.carter.order.OrderBook;
import com.carter.order.OrderPool;
import com.carter.publisher.OrderBookListener;
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

    public MatchingEngine(OrderBookListener listener) {
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
        book.addOrder(orderId, quantity, price, side);
        instrumentIdByOrderId.put(orderId, instrumentId);
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
