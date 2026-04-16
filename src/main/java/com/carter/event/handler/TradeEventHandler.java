package com.carter.event.handler;

import com.carter.event.TradeEvent;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeEventHandler implements EventHandler<TradeEvent> {
    @Override
    public void onEvent(TradeEvent tradeEvent, long sequence, boolean endOfBatch) {
        log.info("TradeEvent received: aggressorOrderId={}, restingOrderId={}, price={}, quantity={}",
                tradeEvent.getAggressorOrderId(),
                tradeEvent.getRestingOrderId(),
                tradeEvent.getPrice(),
                tradeEvent.getQuantity());
    }
}
