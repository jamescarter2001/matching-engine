package com.carter.order;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderRequest {



    private byte requestType;
    private int instrumentId;
    private int price;
    private int quantity;
    private byte side;



}
