package com.carter.order;

import com.carter.publisher.OrderBookListener;
import org.agrona.collections.Long2LongHashMap;

import static com.carter.order.OrderPool.MAX_ORDERS;
import static com.carter.order.OrderSide.isBuy;

public class OrderBook {

    private static final int NULL = -1;

    private final OrderPool orderPool;

    private final OrderBookListener listener;

    private static final int BUY_SIDE = 0;
    private static final int SELL_SIDE = 1;
    private final OrderBookLevel[][] levels;

    private final int minPrice;
    private final int maxPrice;
    private final int tickSize;

    private final Long2LongHashMap idToSlotMap = new Long2LongHashMap(
            MAX_ORDERS,
            0.8f,
            NULL,
            true
    );

    private int bestBidField = Integer.MIN_VALUE;
    private int bestAskField = Integer.MAX_VALUE;

    public OrderBook(OrderPool orderPool, OrderBookListener listener, int minPrice, int maxPrice, int tickSize) {
        this.orderPool = orderPool;
        this.listener = listener;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.tickSize = tickSize;
        int ladderSize = ((maxPrice - minPrice) / tickSize) + 1;
        levels = new OrderBookLevel[ladderSize][2];
        for (int i = 0; i < ladderSize; i++) {
            levels[i][BUY_SIDE] = new OrderBookLevel();
            levels[i][SELL_SIDE] = new OrderBookLevel();
        }
    }

    public void clear() {
        idToSlotMap.clear();
        bestBidField = Integer.MIN_VALUE;
        bestAskField = Integer.MAX_VALUE;
    }

    public int getOrder(long orderId) {
        return (int) idToSlotMap.get(orderId);
    }

    public void addOrder(long orderId, int price, int quantity, byte side) {
        if (price < minPrice || price > maxPrice) {
            listener.onOrderUpdate(orderId, 0, quantity, OrderStatus.REJECTED);
            return;
        }

        int remainder = match(orderId, price, quantity, side);
        int executedQty = quantity - remainder;

        if (remainder == 0) {
            listener.onOrderUpdate(orderId, executedQty, remainder, OrderStatus.FULLY_FILLED);
            return;
        }

        if (remainder < quantity) {
            listener.onOrderUpdate(orderId, executedQty, remainder, OrderStatus.PARTIALLY_FILLED);
        } else {
            listener.onOrderUpdate(orderId, executedQty, remainder, OrderStatus.NEW);
        }

        int slot = orderPool.acquire();
        orderPool.setOrderId(slot, orderId);
        orderPool.setQty(slot, quantity);
        orderPool.setRemainingQty(slot, remainder);
        orderPool.setPrice(slot, price);
        orderPool.setSide(slot, side);

        OrderBookLevel level = isBuy(side) ? getLevelBuy(price) : getLevelSell(price);

        if (level.getHead() == NULL) {
            level.setHead(slot);
        }

        orderPool.setPrevSlot(slot, level.getTail());

        if (level.getTail() != NULL) {
            orderPool.setNextSlot(level.getTail(), slot);
        }

        level.setTail(slot);

        if (isBuy(side)) {
            bestBidField = Math.max(price, bestBidField);
        } else {
            bestAskField = Math.min(price, bestAskField);
        }

        idToSlotMap.put(orderId, slot);
    }

    public void cancelOrder(long orderId) {
        removeOrder(orderId, OrderRemoveReason.CANCELLED);
    }

    public void removeOrder(long orderId, byte reason) {
        int slot = (int) idToSlotMap.remove(orderId);

        if (slot == NULL) {
            return;
        }

        int quantity = orderPool.getQty(slot);
        int remainingQuantity = orderPool.getRemainingQty(slot);
        int executedQty = quantity - remainingQuantity;

        listener.onOrderUpdate(
                orderId,
                executedQty,
                remainingQuantity,
                OrderRemoveReason.isFilled(reason) ?
                        OrderStatus.FULLY_FILLED : OrderStatus.CANCELLED
        );

        removeOrder(slot);
    }

    public void removeOrder(int slot) {
        int prev = orderPool.getPrevSlot(slot);
        int next = orderPool.getNextSlot(slot);

        if (prev != NULL) orderPool.setNextSlot(prev, next);
        if (next != NULL) orderPool.setPrevSlot(next, prev);

        byte side = orderPool.getSide(slot);
        int price = orderPool.getPrice(slot);
        OrderBookLevel level = isBuy(side) ? getLevelBuy(price) : getLevelSell(price);

        if (slot == level.getHead()) {
            level.setHead(next);
        } else if (slot == level.getTail()) {
            level.setTail(prev);
        }

        orderPool.release(slot);
    }

    private int match(long orderId, int price, int quantity, byte side) {
        return isBuy(side) ? matchBuyToSell(orderId, price, quantity) : matchSellToBuy(orderId, price, quantity);
    }

    private int matchBuyToSell(long orderId, int price, int quantity) {
        int bestAsk = getBestAsk();
        if (bestAsk > price) {
            return quantity;
        }

        int remainder = quantity;
        for (int i = bestAsk; i <= price && remainder > 0; i += tickSize) {
            OrderBookLevel level = getLevelSell(i);
            remainder = walkAndMatch(orderId, level, remainder);
        }

        return remainder;
    }

    private int matchSellToBuy(long orderId, int price, int quantity) {
        int bestBid = getBestBid();
        if (bestBid < price) {
            return quantity;
        }

        int remainder = quantity;
        for (int i = bestBid; i >= price && remainder > 0; i -= tickSize) {
            OrderBookLevel level = getLevelBuy(i);
            remainder = walkAndMatch(orderId, level, remainder);
        }

        return remainder;
    }

    private int walkAndMatch(long orderId, OrderBookLevel level, int remainder) {
        int nextTarget = level.getHead();

        while (nextTarget != NULL && remainder > 0) {
            int currentResting = nextTarget;
            nextTarget = orderPool.getNextSlot(currentResting);

            long restingOrderId = orderPool.getOrderId(currentResting);
            int restingQty = orderPool.getRemainingQty(currentResting);
            int restingPrice = orderPool.getPrice(currentResting);

            int matchedQuantity = Math.min(restingQty, remainder);
            remainder = remainder - matchedQuantity;

            int newRemainingQty = restingQty - matchedQuantity;
            orderPool.setRemainingQty(currentResting, newRemainingQty);

            listener.onTrade(orderId, restingOrderId, restingPrice, matchedQuantity);
            if (matchedQuantity == restingQty) {
                removeOrder(restingOrderId, OrderRemoveReason.FILLED);
            } else {
                listener.onOrderUpdate(
                        restingOrderId,
                        orderPool.getQty(currentResting) - newRemainingQty,
                        newRemainingQty,
                        OrderStatus.PARTIALLY_FILLED
                );
            }
        }

        return remainder;
    }

    private int getBestBid() {
        if (bestBidField == Integer.MIN_VALUE) return Integer.MIN_VALUE;
        int bestLevel = priceToLevel(bestBidField);
        if (levels[bestLevel][BUY_SIDE].getHead() == NULL) {
            bestBidField = nextBestBid(bestLevel - 1);
        }
        return bestBidField;
    }

    private int getBestAsk() {
        if (bestAskField == Integer.MAX_VALUE) return Integer.MAX_VALUE;
        int bestLevel = priceToLevel(bestAskField);
        if (levels[bestLevel][SELL_SIDE].getHead() == NULL) {
            bestAskField = nextBestAsk(bestLevel + 1);
        }
        return bestAskField;
    }

    private int nextBestBid(int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (!levels[i][BUY_SIDE].isEmpty()) {
                return levelToPrice(i);
            }
        }
        return Integer.MIN_VALUE;
    }

    private int nextBestAsk(int fromIndex) {
        for (int i = fromIndex; i < levels.length; i++) {
            if (!levels[i][SELL_SIDE].isEmpty()) {
                return levelToPrice(i);
            }
        }
        return Integer.MAX_VALUE;
    }

    private OrderBookLevel getLevelBuy(int price) {
        return getLevel(price, BUY_SIDE);
    }

    private OrderBookLevel getLevelSell(int price) {
        return getLevel(price, SELL_SIDE);
    }

    private OrderBookLevel getLevel(int price, int side) {
        int level = priceToLevel(price);
        return levels[level][side];
    }

    private int priceToLevel(int price) {
        return (price - minPrice) / tickSize;
    }

    private int levelToPrice(int level) {
        return minPrice + level * tickSize;
    }

}
