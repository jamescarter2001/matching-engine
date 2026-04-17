package com.carter.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class OrderBookLevel {

    private final OrderSide side;

    private int quantity;

    private int head = -1;
    private int tail = -1;

    public boolean isEmpty() {
        return head == -1;
    }

}
