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
public class MultiVariatePowerSeriesBenchmarks {

    public static interface JaninoMathFunction {

        double apply(double x1, double x2, double x3, double x4, double x5, double x6, double x7, double x8, double x9, double x10,
                double x11, double x12, double x13, double x14, double x15, double x16, double x17, double x18, double x19, double x20,
                double x21, double x22, double x23, double x24, double x25, double x26, double x27, double x28, double x29, double x30
        );
    }

    private int[] randomData;
    private final AtomicInteger cursor = new AtomicInteger();
    private static final int NUM_VARS = 30;
    private static final String EXPRESSION = seriesGen(NUM_VARS);

    // Pre-compiled instances
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private JaninoMathFunction fastEvaluator;

    private final int[] slots = new int[NUM_VARS];
    private final double[] xValues = new double[NUM_VARS];
    private final double[] turboArgs = new double[NUM_VARS];
    private double[] factorials;   // 1!, 3!, 5!, ..., 59!

    static {
        System.out.println("expression:\n" + EXPRESSION);
    }

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);

        parserNG = new MathExpression(EXPRESSION, true);

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        // Cache slot indices once
        for (int i = 0; i < NUM_VARS; i++) {
            slots[i] = parserNG.getVariable("x" + (i + 1)).getFrameIndex();
        }

        // Janino setup (unchanged, except you can improve MathToJaninoConverter if needed)
        String javaExpression = MathToJaninoConverter.convert(EXPRESSION);
        String classBody = buildJaninoClassBody(javaExpression);

        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{JaninoMathFunction.class});
            cbe.cook(classBody);

            this.fastEvaluator = (JaninoMathFunction) cbe.getClazz()
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
     * Helper to generate the 30 input values from one random seed
     */
    private void generateInputs() {
        double base = randomData[cursor.getAndIncrement() % randomData.length];
        xValues[0] = base;
        for (int i = 1; i < NUM_VARS; i++) {
            xValues[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
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
    public void nativeJava(Blackhole blackhole) {
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
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();

        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }

        double result = wideningBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @Benchmark
    public void janino(Blackhole blackhole) {
        generateInputs();

        double result = fastEvaluator.apply(
                xValues[0], xValues[1], xValues[2], xValues[3], xValues[4],
                xValues[5], xValues[6], xValues[7], xValues[8], xValues[9],
                xValues[10], xValues[11], xValues[12], xValues[13], xValues[14],
                xValues[15], xValues[16], xValues[17], xValues[18], xValues[19],
                xValues[20], xValues[21], xValues[22], xValues[23], xValues[24],
                xValues[25], xValues[26], xValues[27], xValues[28], xValues[29]
        );
        blackhole.consume(result);
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues[0]);
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
                .include(MultiVariatePowerSeriesBenchmarks.class.getSimpleName())
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
    private String buildJaninoClassBody(String javaExpression) {
        return String.format("""
         public double factorial(int n) {
                                           double result = 1;
                                           for (int i = 2; i <= n; i++) {
                                             result *= i;
                                           }
                                           return result;
                                         }                                
          public double eval(double x1, double x2, double x3, double x4, double x5, double x6, double x7, double x8, double x9, double x10,
                                    double x11, double x12, double x13, double x14, double x15, double x16, double x17, double x18, double x19, double x20,
                                    double x21, double x22, double x23, double x24, double x25, double x26, double x27, double x28, double x29, double x30) {
              return %s;
          }
          
          @Override
          public double apply(double x1, double x2, double x3, double x4, double x5, double x6, double x7, double x8, double x9, double x10,
                          double x11, double x12, double x13, double x14, double x15, double x16, double x17, double x18, double x19, double x20,
                          double x21, double x22, double x23, double x24, double x25, double x26, double x27, double x28, double x29, double x30) {
              return eval(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10,
                                        x11, x12, x13, x14, x15, x16, x17, x18, x19, x20,
                                        x21, x22, x23, x24, x25, x26, x27, x28, x29, x30);
          }
          """, javaExpression);
    }

    public static String seriesGen(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int k = 2 * i + 1;
            String term = (i == 0 ? "" : (i % 2 == 0 ? " + " : " - "))
                    + "(x" + (i + 1) + "^" + k + ")/" + k + "!";
            sb.append(term);
        }
        return sb.toString();
    }

    public static String coSeriesGen(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int k = 2 * i;
            String term = (i == 0 ? "" : (i % 2 == 0 ? " + " : " - "))
                    + "(x" + (i + 1) + "^" + k + ")/" + k + "!";
            sb.append(term);
        }
        return sb.toString();
    }
}
