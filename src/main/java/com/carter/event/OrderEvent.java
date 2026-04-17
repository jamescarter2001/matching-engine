package com.carter.event;

import com.carter.order.OrderSide;
import com.carter.order.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderEvent {

    private long orderId;
    private OrderSide side;
    private int executedQty;
    private int remainingQty;
    private OrderStatus status;

}
