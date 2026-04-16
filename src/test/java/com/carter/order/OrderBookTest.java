package com.carter.order;

import com.carter.publisher.OrderBookListener;
import org.agrona.collections.LongArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.carter.order.OrderPool.MAX_ORDERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderBookTest {

    private final long orderId1 = 1;
    private final long orderId2 = 2;
    private final long orderId3 = 3;

    private final List<TradeEvent> tradeEvents = new ArrayList<>();
    private final List<OrderEvent> orderEvents = new ArrayList<>();
    private final LongArrayList restingFilledEvents = new LongArrayList();

    private final OrderBookListener listener = new OrderBookListener() {
        @Override
        public void onOrderUpdate(long orderId, byte side, int executedQty, int remainingQty, byte status) {
            orderEvents.add(new OrderEvent(orderId, side, executedQty, remainingQty, status));
        }

        @Override
        public void onRestingOrderFilled(long orderId) {
            restingFilledEvents.add(orderId);
        }

        @Override
        public void onTrade(long aggressorOrderId, long restingOrderId, int price, int quantity) {
            tradeEvents.add(new TradeEvent(aggressorOrderId, restingOrderId, price, quantity));
        }
    };

    private final OrderPool orderPool = new OrderPool();
    final OrderBook underTest = new OrderBook(orderPool, listener, 10, 30, 1);

    @AfterEach
    void afterEach() {
        underTest.clear();
        orderPool.clear();
        tradeEvents.clear();
        orderEvents.clear();
        restingFilledEvents.clear();
    }

    @Nested
    class BestPrices {

        @Test
        void bestBid() {
            assertThat(underTest.getBestBid()).isEqualTo(Integer.MIN_VALUE);

            underTest.addOrder(orderId1, 15, 10, OrderSide.BUY);
            assertThat(underTest.getBestBid()).isEqualTo(15);

            underTest.addOrder(orderId2, 10, 50, OrderSide.BUY);
            assertThat(underTest.getBestBid()).isEqualTo(15);

            underTest.addOrder(orderId3, 10, 10, OrderSide.SELL);
            assertThat(underTest.getBestBid()).isEqualTo(10);
        }

        @Test
        void bestAsk() {
            assertThat(underTest.getBestAsk()).isEqualTo(Integer.MAX_VALUE);

            underTest.addOrder(orderId1, 15, 10, OrderSide.SELL);
            assertThat(underTest.getBestAsk()).isEqualTo(15);

            underTest.addOrder(orderId2, 10, 10, OrderSide.SELL);
            assertThat(underTest.getBestAsk()).isEqualTo(10);

            underTest.addOrder(orderId3, 20, 10, OrderSide.BUY);
            assertThat(underTest.getBestAsk()).isEqualTo(15);
        }

    }

    @Nested
    class AddOrder {

        @Test
        void addThreeSameLevel() {
            underTest.addOrder(orderId1, 10, 20, OrderSide.BUY);
            int slot1 = underTest.getOrder(orderId1);

            assertThat(slot1).isNotEqualTo(-1);

            assertThat(orderPool.getPrice(slot1)).isEqualTo(10);
            assertThat(orderPool.getQty(slot1)).isEqualTo(20);
            assertThat(orderPool.getSide(slot1)).isEqualTo(OrderSide.BUY);
            assertThat(orderPool.getPrevSlot(slot1)).isEqualTo(-1);
            assertThat(orderPool.getNextSlot(slot1)).isEqualTo(-1);

            underTest.addOrder(orderId2, 10, 40, OrderSide.BUY);
            int slot2 = underTest.getOrder(orderId2);

            assertThat(slot2).isNotEqualTo(-1);

            assertThat(orderPool.getPrice(slot2)).isEqualTo(10);
            assertThat(orderPool.getQty(slot2)).isEqualTo(40);
            assertThat(orderPool.getSide(slot2)).isEqualTo(OrderSide.BUY);
            assertThat(orderPool.getPrevSlot(slot2)).isEqualTo(slot1);
            assertThat(orderPool.getNextSlot(slot2)).isEqualTo(-1);

            assertThat(orderPool.getNextSlot(slot1)).isEqualTo(slot2);

            underTest.addOrder(orderId3, 10, 60, OrderSide.BUY);
            int slot3 = underTest.getOrder(orderId3);

            assertThat(slot3).isNotEqualTo(-1);

            assertThat(orderPool.getPrice(slot3)).isEqualTo(10);
            assertThat(orderPool.getQty(slot3)).isEqualTo(60);
            assertThat(orderPool.getSide(slot3)).isEqualTo(OrderSide.BUY);
            assertThat(orderPool.getPrevSlot(slot3)).isEqualTo(slot2);
            assertThat(orderPool.getNextSlot(slot3)).isEqualTo(-1);
        }

        @Test
        void addThreeCancelOne() {
            underTest.addOrder(orderId1, 10, 10, OrderSide.BUY);
            int slot1 = underTest.getOrder(orderId1);
            underTest.addOrder(orderId2, 10, 10, OrderSide.BUY);
            int slot2 = underTest.getOrder(orderId2);
            underTest.addOrder(orderId3, 10, 10, OrderSide.BUY);
            int slot3 = underTest.getOrder(orderId3);

            assertThat(orderPool.getNextSlot(slot1)).isEqualTo(slot2);
            assertThat(orderPool.getNextSlot(slot2)).isEqualTo(slot3);

            underTest.cancelOrder(orderId2);

            assertThat(orderPool.getNextSlot(slot1)).isEqualTo(slot3);
            assertThat(orderPool.getNextSlot(slot3)).isEqualTo(-1);
        }

        @Test
        void maxAllocations() {
            for (int i = 0; i < MAX_ORDERS; i++) {
                underTest.addOrder(i, 10, Math.max(i, 1), OrderSide.BUY);
            }

            for (int i = 0; i < MAX_ORDERS; i++) {
                assertThat(orderPool.getQty(i)).isEqualTo(Math.max(i, 1));
            }

            assertThatThrownBy(() -> underTest.addOrder(MAX_ORDERS+1, 11, 10, OrderSide.BUY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Pool exhausted");
        }

    }

    @Nested
    class MatchOrder {

        @Test
        void oneForOne() {
            underTest.addOrder(orderId1, 10, 20, OrderSide.BUY);
            underTest.addOrder(orderId2, 10, 20, OrderSide.SELL);
            assertThat(tradeEvents).containsOnly(new TradeEvent(orderId2, orderId1, 10, 20));
        }

        @Test
        void restingPriceWinsBuyerFirst() {
            underTest.addOrder(orderId1, 15, 30, OrderSide.BUY);
            underTest.addOrder(orderId2, 12, 30, OrderSide.SELL);
            assertThat(tradeEvents).containsOnly(new TradeEvent(orderId2, orderId1, 15, 30));
        }

        @Test
        void restingPriceWinsSellerFirst() {
            underTest.addOrder(orderId1, 12, 30, OrderSide.SELL);
            underTest.addOrder(orderId2, 15, 30, OrderSide.BUY);
            assertThat(tradeEvents).containsOnly(new TradeEvent(orderId2, orderId1, 12, 30));
        }

        @Test
        void bestPriceWinsMultipleSellers() {
            underTest.addOrder(orderId1, 15, 30, OrderSide.SELL);
            underTest.addOrder(orderId2, 14, 30, OrderSide.SELL);
            underTest.addOrder(orderId3, 18, 30, OrderSide.BUY);
            assertThat(tradeEvents).containsOnly(new TradeEvent(orderId3, orderId2, 14, 30));
        }

        @Test
        void partialFill() {
            underTest.addOrder(orderId1, 10, 30, OrderSide.BUY);
            underTest.addOrder(orderId2, 10, 20, OrderSide.SELL);
            underTest.addOrder(orderId3, 10, 10, OrderSide.SELL);
            assertThat(orderEvents).containsExactly(
                    new OrderEvent(orderId1, OrderSide.BUY, 0, 30, OrderStatus.NEW),
                    new OrderEvent(orderId2, OrderSide.SELL, 0, 20, OrderStatus.NEW),
                    new OrderEvent(orderId1, OrderSide.BUY, 20, 10, OrderStatus.PARTIALLY_FILLED),
                    new OrderEvent(orderId2, OrderSide.SELL, 20, 0, OrderStatus.FULLY_FILLED),
                    new OrderEvent(orderId3, OrderSide.SELL, 0, 10, OrderStatus.NEW),
                    new OrderEvent(orderId1, OrderSide.BUY, 30, 0, OrderStatus.FULLY_FILLED),
                    new OrderEvent(orderId3, OrderSide.SELL, 10, 0, OrderStatus.FULLY_FILLED)
            );
            assertThat(tradeEvents).containsExactly(
                    new TradeEvent(orderId2, orderId1, 10, 20),
                    new TradeEvent(orderId3, orderId1, 10, 10)
            );
            assertThat(restingFilledEvents).containsOnly(orderId1);
        }

    }

    private record TradeEvent(long aggressorOrderId, long restingOrderId, int price, int quantity) {}
    private record OrderEvent(long orderId, byte side, int executedQty, int remainingQty, byte status) {}

}