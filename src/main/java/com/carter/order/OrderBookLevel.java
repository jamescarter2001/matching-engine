package com.carter.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class OrderBookLevel {

    private int quantity;

    private int head = -1;
    private int tail = -1;

    public boolean isEmpty() {
        return head == -1;
    }

}
