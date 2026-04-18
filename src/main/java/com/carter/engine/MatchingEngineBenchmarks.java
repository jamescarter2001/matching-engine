package com.carter.engine;

import com.carter.listener.DefaultMatchingEngineListener;
import com.carter.order.OrderSide;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

public class MatchingEngineBenchmarks {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private MatchingEngine matchingEngine = new MatchingEngine(new DefaultMatchingEngineListener());

        @Param({"1000", "2000", "3000"})
        private int price;

        @Param({"10", "100", "1000"})
        private int quantity;

        @Param({"1", "2"})
        private long instrumentId;

        @TearDown(Level.Iteration)
        public void tearDown() {
            matchingEngine.clear();
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @BenchmarkMode(Mode.SampleTime)
    @Fork(1)
    @Warmup(iterations = 5, time = 3)
    @Measurement(iterations = 5, time = 5)
    public long addOrder(BenchmarkState state) {
        state.matchingEngine.addOrder(state.instrumentId, state.price, state.quantity, OrderSide.BUY);
        return state.matchingEngine.addOrder(state.instrumentId, state.price, state.quantity, OrderSide.SELL);
    }

}
