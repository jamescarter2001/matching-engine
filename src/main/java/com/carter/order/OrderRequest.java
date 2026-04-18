package com.carter.order;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.co.real_logic.sbe.benchmarks.fix.Side;

@Data
@NoArgsConstructor
public class OrderRequest {

    private byte requestType;
    private int instrumentId;
    private int price;
    private int quantity;
    private Side side;

}
