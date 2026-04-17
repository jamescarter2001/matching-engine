package com.carter;

import com.carter.engine.MatchingEngine;
import com.carter.listener.DefaultMatchingEngineListener;
import com.carter.order.OrderSide;

public class MatchingEngineApplication {

    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine(new DefaultMatchingEngineListener());
        engine.addOrder(1, 1001, 10, OrderSide.BUY);
        engine.addOrder(1, 1000, 10, OrderSide.SELL);

        // temp
        while (true) {
        }
    }
}