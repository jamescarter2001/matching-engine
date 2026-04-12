package com.carter.order;

import com.carter.publisher.OrderBookListener;
import org.agrona.collections.Long2LongHashMap;

import static com.carter.order.OrderPool.MAX_ORDERS;
import static com.carter.order.OrderSide.isBuy;

public class OrderBook {

    private static final int NULL = -1;
    private static final byte BUY_SIDE = 0;
    private static final byte SELL_SIDE = 1;

    private final OrderPool orderPool;
    private final OrderBookLevel[][] levels;
    private final OrderBookListener listener;

    private final int minPrice;
    private final int maxPrice;
    private final int tickSize;
    private final int levelCount;

    private final Long2LongHashMap idToSlotMap = new Long2LongHashMap(
            MAX_ORDERS,
            0.8f,
            NULL,
            true
    );

    private int bestBid = NULL;
    private int bestAsk = NULL;

    public OrderBook(OrderPool orderPool, OrderBookListener listener, int minPrice, int maxPrice, int tickSize) {
        this.orderPool = orderPool;
        this.listener = listener;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.tickSize = tickSize;
        int ladderSize = ((maxPrice - minPrice) / tickSize) + 1;
        this.levelCount = ladderSize;
        levels = new OrderBookLevel[ladderSize][2];
        for (int i = 0; i < ladderSize; i++) {
            levels[i][BUY_SIDE] = new OrderBookLevel();
            levels[i][SELL_SIDE] = new OrderBookLevel();
        }
    }

    public void clear() {
        idToSlotMap.clear();
        bestBid = NULL;
        bestAsk = NULL;
    }

    public int getBestBid() {
        if (bestBid == NULL) {
            return Integer.MIN_VALUE;
        }
        return levelToPrice(bestBid);
    }

    public int getBestAsk() {
        if (bestAsk == NULL) {
            return Integer.MAX_VALUE;
        }
        return levelToPrice(bestAsk);
    }

    public int getOrder(long orderId) {
        return (int) idToSlotMap.get(orderId);
    }

    public void addOrder(long orderId, int price, int quantity, byte side) {
        if (price < minPrice || price > maxPrice) {
            listener.onOrderUpdate(orderId, 0, quantity, OrderStatus.REJECTED);
            return;
        }

        listener.onOrderUpdate(orderId, 0, quantity, OrderStatus.NEW);

        int remainder = match(orderId, price, quantity, side);
        int executedQty = quantity - remainder;

        if (remainder == 0) {
            listener.onOrderUpdate(orderId, executedQty, remainder, OrderStatus.FULLY_FILLED);
            return;
        }

        if (remainder < quantity) {
            listener.onOrderUpdate(orderId, executedQty, remainder, OrderStatus.PARTIALLY_FILLED);
        }

        int slot = orderPool.acquire();
        orderPool.setOrderId(slot, orderId);
        orderPool.setQty(slot, quantity);
        orderPool.setRemainingQty(slot, remainder);
        orderPool.setPrice(slot, price);
        orderPool.setSide(slot, side);

        int level = priceToLevel(price);
        OrderBookLevel levelQueue = isBuy(side) ? getLevelBuy(level) : getLevelSell(level);

        if (levelQueue.getHead() == NULL) {
            levelQueue.setHead(slot);
        }

        orderPool.setPrevSlot(slot, levelQueue.getTail());

        if (levelQueue.getTail() != NULL) {
            orderPool.setNextSlot(levelQueue.getTail(), slot);
        }

        levelQueue.setTail(slot);

        if (isBuy(side)) {
            bestBid = bestBid != NULL ? Math.max(level, bestBid) : level;
        } else {
            bestAsk = bestAsk != NULL ? Math.min(level, bestAsk) : level;
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

    private void removeOrder(int slot) {
        int prev = orderPool.getPrevSlot(slot);
        int next = orderPool.getNextSlot(slot);

        if (prev != NULL) orderPool.setNextSlot(prev, next);
        if (next != NULL) orderPool.setPrevSlot(next, prev);

        byte side = orderPool.getSide(slot);
        int level = priceToLevel(orderPool.getPrice(slot));
        OrderBookLevel levelQueue = isBuy(side) ? getLevelBuy(level) : getLevelSell(level);

        if (slot == levelQueue.getHead()) levelQueue.setHead(next);

        if (slot == levelQueue.getTail()) levelQueue.setTail(prev);

        if (levelQueue.isEmpty()) {
            if (isBuy(side)) {
                bestBid = computeBestBid(level-1);
            } else {
                bestAsk = computeBestAsk(level+1);
            }
        }

        orderPool.release(slot);
    }

    private int match(long orderId, int price, int quantity, byte side) {
        return isBuy(side) ? matchBuyToSell(orderId, price, quantity) : matchSellToBuy(orderId, price, quantity);
    }

    private int matchBuyToSell(long orderId, int price, int quantity) {
        int level = priceToLevel(price);
        if (bestAsk == -1 || bestAsk > level) {
            return quantity;
        }

        int remainder = quantity;
        for (int i = bestAsk; i <= level && remainder > 0; i++) {
            OrderBookLevel levelQueue = getLevelSell(i);
            remainder = walkAndMatch(orderId, levelQueue, remainder);
        }

        return remainder;
    }

    private int matchSellToBuy(long orderId, int price, int quantity) {
        int level = priceToLevel(price);
        if (bestBid == -1 || bestBid < level) {
            return quantity;
        }

        int remainder = quantity;
        for (int i = bestBid; i >= level && remainder > 0; i--) {
            OrderBookLevel levelQueue = getLevelBuy(i);
            remainder = walkAndMatch(orderId, levelQueue, remainder);
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

    private int computeBestBid(int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (!getLevelBuy(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int computeBestAsk(int fromIndex) {
        for (int i = fromIndex; i < levelCount; i++) {
            if (!getLevelSell(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private OrderBookLevel getLevelBuy(int level) {
        return getLevel(level, BUY_SIDE);
    }

    private OrderBookLevel getLevelSell(int level) {
        return getLevel(level, SELL_SIDE);
    }

    private OrderBookLevel getLevel(int level, byte side) {
        return levels[level][side];
    }

    private int priceToLevel(int price) {
        return (price - minPrice) / tickSize;
    }

    private int levelToPrice(int level) {
        return (level + minPrice) * tickSize;
    }

}
