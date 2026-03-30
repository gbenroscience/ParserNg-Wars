package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.objecthunter.exp4j.Expression;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Build with: mvn clean verify -U Run with: java -jar target/benchmarks.jar
 * ".*MultiVariatePowerSeriesBenchmarks.*" JMH Benchmark comparing ParserNG
 * Standard, ParserNG Turbo(Array based), ParserNG Turbo(Widening based), and
 * Janino evaluation of the same pre-compiled expression. JMH Benchmark
 * comparing ParserNG Standard, ParserNG Turbo (Array), ParserNG Turbo
 * (Widening), and Janino for a large multivariate power series expression with
 * 30 variables.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 5)
@Fork(value = 2, warmups = 1)
@Threads(1)
public class StressTestBenchmark {

    public interface JaninoArrayFunction {

        double apply(double[] x);
    }

    private int[] randomData;
    private final AtomicInteger cursor = new AtomicInteger();
    private static final int NUM_VARS = 500;
    private static final String EXPRESSION = stressSeries(NUM_VARS);

    // Pre-compiled instances
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private JaninoArrayFunction fastEvaluator;

    private final int[] slots = new int[NUM_VARS];
    private final double[] xValues = new double[NUM_VARS];
    private final double[] turboArgs = new double[NUM_VARS];
    private double[] factorials;   // 1!, 3!, 5!, ..., 59!

    static {
     //   System.out.println("expression:\n" + EXPRESSION);
    }

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);

        parserNG = new MathExpression(EXPRESSION, true);

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        // Cache slot indices once
        for (int i = 0; i < NUM_VARS; i++) {
            slots[i] = parserNG.getVariable("x" + (i + 1)).getFrameIndex();
        }

        // Janino setup (unchanged, except you can improve MathToJaninoConverter if needed)
        String javaExpression = MathToJaninoConverter.convert(EXPRESSION);
        String classBody = buildJaninoArrayClassBody(NUM_VARS, javaExpression);

        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{JaninoArrayFunction.class});
            cbe.cook(classBody);

            this.fastEvaluator = (JaninoArrayFunction) cbe.getClazz()
                    .getDeclaredConstructor().newInstance();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, "Janino ClassBody Setup Failed", ex);
        }

        // Precompute odd factorials once (same as other engines do at compile time)
        this.factorials = new double[NUM_VARS];
        factorials[0] = 1.0;                    // 1!
        for (int i = 1; i < NUM_VARS; i++) {
            int k = 2 * i + 1;                  // 3, 5, 7, ..., 59
            factorials[i] = factorials[i - 1] * (k - 1) * k;
        }

        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
    }

    /**
     * Helper to generate the {@linkplain StressTestBenchmark#NUM_VARS} input
     * values from one random seed
     */
    private void generateInputs() {
        for (int i = 0; i < NUM_VARS; i++) {
            // Pull a new digit/seed for EVERY variable
            int seed = randomData[cursor.getAndIncrement() % randomData.length];
            xValues[i] = seed * 0.12345;
        }

    }

    // ==================== BENCHMARKS ====================
    @Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs();

        // Fast slot updates using arrays
        for (int i = 0; i < NUM_VARS; i++) {
           parserNG.updateSlot(slots[i], xValues[i]);
        }

        double result = parserNG.solveGeneric().scalar;
        blackhole.consume(result);
    }

    @Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs();

        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }

        double result = arrayBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @Benchmark
    public void nativeJavaWithPow(Blackhole blackhole) {
        generateInputs();

        double result = 0.0;

        for (int i = 0; i < NUM_VARS; i++) {
            int exponent = 2 * i + 1;
            double sign = (i % 2 == 0) ? 1.0 : -1.0;
            double term = sign * Math.pow(xValues[i], exponent) / factorials[i];
            result += term;
        }

        blackhole.consume(result);
    }

    @Benchmark
    public void nativeJavaOptimized(Blackhole blackhole) {
        generateInputs();

        double result = 0.0;
        double sign = 1.0; // Start positive

        // Optimized loop: manually unrolled by 2 to reduce branch checks
        for (int i = 0; i < NUM_VARS; i++) {
            double x = xValues[i];

            // 1. Replaced Math.pow(x, 2) with x * x
            // 2. Used the 'sign' toggle to avoid i % 2 division
            double term = (sign * (x * x)) / factorials[i];

            result += term;
            sign = -sign; // Flip the sign for the next term
        }

        blackhole.consume(result);
    }

    @Benchmark
    public void janino(Blackhole blackhole) {
        generateInputs();

        double result = fastEvaluator.apply(xValues);
        blackhole.consume(result);
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        long checksum = 0;
        for (double v : xValues) {
            checksum ^= Double.doubleToRawLongBits(v);
        }
        blackhole.consume(checksum);
    }

    // ==================== MAIN ====================
    public static void main(String[] args) throws RunnerException {

        /*
          Options opt = new OptionsBuilder()
            .include(MultiVariatePowerSeriesBenchmarks.class.getSimpleName())
            // First Mode: Latency (How fast is one?)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            // Second Mode: Throughput (How many can we do?)
            .mode(Mode.Throughput)
            .timeUnit(TimeUnit.SECONDS) 
            .warmupIterations(5)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(2))
            .forks(1) 
            .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
            // Ensuring the JVM is in "High Performance" mode
            .jvmArgs("-Xms2g", "-Xmx2g", "-XX:+UseParallelGC", "-XX:+TieredCompilation")
            .build();
         */
        Options opt = new OptionsBuilder()
                .include(StressTestBenchmark.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(2))
                .forks(1)
                .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
                .jvmArgs("-Xms2g", "-Xmx2g", "-XX:+UseParallelGC", "-XX:+TieredCompilation")
                .build();

        new Runner(opt).run();
    }

    // ==================== HELPERS ====================
    private String buildJaninoArrayClassBody(int n, String javaExpression) {
        // Manually map array indices to the expression (e.g., x1 -> x[0], x2 -> x[1])
        String mappedExpression = javaExpression;
        for (int i = n; i >= 1; i--) { // Reverse order to avoid partial string replacement
            mappedExpression = mappedExpression.replace("x" + i, "x[" + (i - 1) + "]");
        }

        return String.format("""
        @Override
        public double apply(double[] x) {
            return %s;
        }
        """, mappedExpression);
    }

    public static String stressSeries(int n) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; i++) {

            // Using (i+1) instead of factorial to prevent overflow
            String term = (i == 0 ? "" : " + ") + "(x" + (i + 1) + "^2)/" + (i + 1);

            sb.append(term);

        }

        return sb.toString();

    }
    
    
    
}
