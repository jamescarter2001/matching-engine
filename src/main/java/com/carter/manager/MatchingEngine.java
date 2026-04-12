package com.carter.manager;

import com.carter.order.OrderBook;
import com.carter.order.OrderPool;
import com.carter.publisher.OrderBookListener;
import com.carter.publisher.OrderEventPublisher;
import com.carter.publisher.TradePublisher;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;

public class MatchingEngine {

    private final Long2ObjectHashMap<OrderBook> books = new Long2ObjectHashMap<>();
    private final Long2LongHashMap orderIdToSymbolId = new Long2LongHashMap(-1);
    private final OrderPool orderPool = new OrderPool();

    private long nextOrderId = 0;

    public MatchingEngine(OrderBookListener listener) {
        books.put(1, new OrderBook(orderPool, listener, 1000, 3000, 1));
        books.put(2, new OrderBook(orderPool, listener, 0, 1000, 1));
    }

    public long addOrder(long symbolId, int price, int quantity, byte side) {
        long orderId = nextOrderId++;
        OrderBook book = books.get(symbolId);
        book.addOrder(orderId, quantity, price, side);
        orderIdToSymbolId.put(orderId, symbolId);
        return orderId;
    }

    public void cancelOrder(long orderId) {
        long symbolId = orderIdToSymbolId.remove(orderId);
        OrderBook book = books.get(symbolId);
        book.cancelOrder(orderId);
    }

}
