package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import com.github.gbenroscience.parser.wars.Stats;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger; 
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.annotations.*;

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*NativeJava.*" 
 * @author GBEMIRO
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, warmups = 1)
@Threads(1)
//@Profiler(GCProfiler.class)
public class NativeJava {

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//
    // The expression to benchmark 

    private static final String EXPRESSION = ParserNGWars.getExpression(); 

    private static final String[] expressionVars = ParserNGWars.getVars(EXPRESSION);

    private static final int NUM_VARS = expressionVars.length;
    private final double[] xValues = new double[NUM_VARS];
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;


    double[] turboArgs = new double[NUM_VARS];
    private final int[] slots = new int[NUM_VARS];

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        parserNG = new MathExpression(EXPRESSION, true);

        // Cache slot indices once
        for (int i = 0; i < NUM_VARS; i++) {
            slots[i] = parserNG.getVariable("x" + (i + 1)).getFrameIndex();
        }

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(NativeJava.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
  
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            parserNG.updateSlot(slots[i], xValues[i]);
        }
        double result = parserNG.solveGeneric().scalar;
        blackhole.consume(result);
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }
        double result = arrayBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }
        double result = wideningBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);   
    }

 
    
       @Benchmark
    public void nativeJava(Blackhole bh) {
         generateInputs();
        double x1 = xValues[0];
        double x2 = xValues[1];
        
        double result = Math.sin(Math.sqrt(x1 * x1 + x2 * x2));
        bh.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NativeJava.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupIterations(5)
                .warmupTime(TimeValue.milliseconds(200L))
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(500))
                .forks(2)
                .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
                .jvmArgs("-Xms2g", "-Xmx2g") // tune heap if needed
                .build();

        new Runner(opt).run();
    }

    private void generateInputs() {
        double base = randomData[cursor.getAndIncrement() % randomData.length];
        if (xValues.length != 0) {
            xValues[0] = base;
        }
        for (int i = 1; i < NUM_VARS; i++) {
            xValues[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
    }

}
