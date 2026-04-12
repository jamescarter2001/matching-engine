package com.carter.runner;

import com.carter.engine.MatchingEngineBenchmarks;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {

    public static void main() throws Exception {
        Options opt = new OptionsBuilder()
                .include(MatchingEngineBenchmarks.class.getSimpleName())
                // .jvmArgsAppend("-Xlog:safepoint*=info")
                // .jvmArgsAppend("-XX:+UseZGC")
                // .result("results.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(opt).run();
    }

}
