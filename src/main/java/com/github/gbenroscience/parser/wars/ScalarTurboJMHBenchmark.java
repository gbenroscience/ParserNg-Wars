package com.github.gbenroscience.parser.wars;

/**
 *
 * @author GBEMIRO
 */ 

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays; 


 
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * JMH Benchmark to definitively compare Array-Based vs Widening variable passing
 * inside the compiled FastCompositeExpression.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ScalarTurboJMHBenchmark {

    // We sweep from 1 to 40 variables to watch the JVM calling convention break down
    //@Param({"1", "5", "10", "15", "20", "30", "40"})
    @Param({"50", "60","63"})
    public int varCount;

    private FastCompositeExpression arrayEvaluator;
    private FastCompositeExpression wideningEvaluator;
    private double[] vars;

    @Setup(Level.Trial)
    public void setup() throws Throwable {
        // 1. Build a pure overhead expression: x0 + x1 + x2 ...
        StringBuilder sb = new StringBuilder("x0");
        for (int i = 1; i < varCount; i++) {
            sb.append("+x").append(i);
        }
        String expr = sb.toString();

        // 2. Initialize the parser (No constant folding to isolate variable passing)
        MathExpression me = new MathExpression(expr, false);

        // 4. Compile both strategies using the wrapper we refined
        arrayEvaluator = new ScalarTurboEvaluator(me, false).compile();
        wideningEvaluator = new ScalarTurboEvaluator(me, true).compile();

        // 5. Setup the execution arguments
        vars = new double[varCount];
        Arrays.fill(vars, 1.5);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testArrayBased(Blackhole bh) throws Throwable {
        // Evaluator 1 path
        bh.consume(arrayEvaluator.applyScalar(vars));
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testWidening(Blackhole bh) throws Throwable {
        // Evaluator 2 path
        bh.consume(wideningEvaluator.applyScalar(vars));
    }
    
     public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ScalarTurboJMHBenchmark.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupIterations(5)
                .warmupTime(TimeValue.milliseconds(200L))
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(1000))
                .forks(2)
                .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
                .jvmArgs("-Xms2g", "-Xmx2g") // tune heap if needed
                .build();

        new Runner(opt).run();
    }
}