package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator; 
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

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
@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 1)
@Threads(1)
public class NativeJava {

    private static final String EXPRESSION = "sin(sqrt(x1^2 + x2^2))";
    
    // Pre-generated inputs - large enough to avoid cache effects
    private static final int INPUT_SIZE = 100_000;
    private double[] x1Values = new double[INPUT_SIZE];
    private double[] x2Values = new double[INPUT_SIZE];

    private int inputIndex = 0;

    // ParserNG components
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private final double[] turboArgs = new double[2];
    private final int[] slots = new int[2];

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);

        // === Pre-generate inputs ===
        long seed = System.currentTimeMillis();
        for (int i = 0; i < INPUT_SIZE; i++) {
            double base = (seed + i) % 1000 * 0.017;   // varied but predictable values
            x1Values[i] = base;
            x2Values[i] = base + 0.5 + (i % 17) * 0.1;
        }

        // === ParserNG setup ===
        parserNG = new MathExpression(EXPRESSION, true);
        slots[0] = parserNG.getVariable("x1").getFrameIndex();
        slots[1] = parserNG.getVariable("x2").getFrameIndex();

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private void nextInput() {
        inputIndex = (inputIndex + 1) % INPUT_SIZE;
    }

    // ===================================================================
    // ====================== BENCHMARKS =================================
    // ===================================================================

    @Benchmark
    public void baseline(Blackhole bh) {
        nextInput();
        bh.consume(x1Values[inputIndex]);   // minimal overhead
    }

    @Benchmark
    public void nativeJava(Blackhole bh) {
        nextInput();
        double x1 = x1Values[inputIndex];
        double x2 = x2Values[inputIndex];
        
        double result = Math.sin(Math.sqrt(x1 * x1 + x2 * x2));
        bh.consume(result);
    }

    @Benchmark
    public void parserNg(Blackhole bh) {
        nextInput();
        double x1 = x1Values[inputIndex];
        double x2 = x2Values[inputIndex];

        parserNG.updateSlot(slots[0], x1);
        parserNG.updateSlot(slots[1], x2);
        
        double result = parserNG.solveGeneric().scalar;
        bh.consume(result);
    }

    @Benchmark
    public void parserNgTurboArrayBased(Blackhole bh) {
        nextInput();
        double x1 = x1Values[inputIndex];
        double x2 = x2Values[inputIndex];

        turboArgs[slots[0]] = x1;
        turboArgs[slots[1]] = x2;

        double result = arrayBasedTurbo.applyScalar(turboArgs);
        bh.consume(result);
    }

    @Benchmark
    public void parserNgTurboWideningBased(Blackhole bh) {
        nextInput();
        double x1 = x1Values[inputIndex];
        double x2 = x2Values[inputIndex];

        turboArgs[slots[0]] = x1;
        turboArgs[slots[1]] = x2;

        double result = wideningBasedTurbo.applyScalar(turboArgs);
        bh.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NativeJava.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.milliseconds(200))
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(500))
                .forks(2)
                .addProfiler("gc")
                .build();

        new Runner(opt).run();
    }
}