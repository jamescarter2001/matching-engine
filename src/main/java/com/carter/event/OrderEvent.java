package com.carter.event;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderEvent {

    private long orderId;
    private byte side;
    private int executedQty;
    private int remainingQty;
    private byte status;

}
