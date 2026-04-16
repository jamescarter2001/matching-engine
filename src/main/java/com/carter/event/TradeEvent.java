package com.carter.event;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TradeEvent {

    private long aggressorOrderId;
    private long restingOrderId;
    private int price;
    private int quantity;

}
