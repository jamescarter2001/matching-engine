package com.carter.runner;

import com.carter.order.OrderPool;
import org.openjdk.jmh.Main;

public class BenchmarkRunner {

    private final OrderPool pool = new OrderPool();

    public static void main(String[] args) throws Exception {
        Main.main(args);
    }

}
