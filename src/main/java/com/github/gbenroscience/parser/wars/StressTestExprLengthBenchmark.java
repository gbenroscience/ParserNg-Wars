package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator1; // Added '1'
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator2;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Uses a long expression repeating just 2 variables, which are not nested
 *
 * @author GBEMIRO
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1)
public class StressTestExprLengthBenchmark {

    public interface JaninoArrayFunction {

        double apply(double[] x);
    }

    private static final int DEPTH = 5; // THE ACTUAL STRESS PARAMETER
    private static final int NUM_VARS = 2;
    private static final String EXPRESSION = generateDeepExpression1(DEPTH);

    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private JaninoArrayFunction fastEvaluator;

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();
    int dataLen = 0;

    private int[] slots = new int[NUM_VARS];
    private double[] xValues = new double[NUM_VARS];
    private double[] turboArgs; // Size will be determined by parser frame size

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);
        parserNG = new MathExpression(EXPRESSION, true);

        try {
            // Using your uploaded evaluator class
            arrayBasedTurbo = new ScalarTurboEvaluator1(parserNG).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator2(parserNG).compile();
            // Initialize turboArgs based on the total frame size needed by the expression
            this.turboArgs = new double[parserNG.getRegistry().size()];
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        slots[0] = parserNG.getVariable("x").getFrameIndex();
        slots[1] = parserNG.getVariable("y").getFrameIndex();

        // Setup Janino
        setupJanino();

        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        dataLen = randomData.length;
    }

    private void setupJanino() {
        // Convert ParserNG syntax to Java syntax
        String javaExpr = MathToJaninoConverter.convert(EXPRESSION);
        // Safe mapping for 2 variables
        javaExpr = javaExpr.replace("x", "v[0]").replace("y", "v[1]");

        String classBody = String.format("""
            @Override
            public double apply(double[] v) {
                return %s;
            }
            """, javaExpr);

        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{JaninoArrayFunction.class});
            cbe.cook(classBody);
            this.fastEvaluator = (JaninoArrayFunction) cbe.getClazz()
                    .getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues[0]);
    }

    private void generateInputs() {
        double base = randomData[cursor.getAndIncrement() % randomData.length];
        xValues[0] = base;
        for (int i = 1; i < NUM_VARS; i++) {
            xValues[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
    }

    @Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs();
        // Fast slot updates using arrays
        for (int i = 0; i < NUM_VARS; i++) {
            parserNG.updateSlot(slots[i], xValues[i]);
        } 
        blackhole.consume(parserNG.solveGeneric().scalar);
    }

    @Benchmark
    public void parserNgTurbo(Blackhole blackhole) {
        generateInputs();
        turboArgs[slots[0]] = xValues[0];
        turboArgs[slots[1]] = xValues[1];
        blackhole.consume(arrayBasedTurbo.applyScalar(turboArgs));
    }

    @Benchmark
    public void parserNgTurboWidening(Blackhole blackhole) {
        generateInputs();
        turboArgs[slots[0]] = xValues[0];
        turboArgs[slots[1]] = xValues[1];
        double x = wideningBasedTurbo.applyScalar(turboArgs);
        if (i == 100) {
            System.out.println("x = " + x);
        }
        blackhole.consume(x);
    }

    int i = 0;

    @Benchmark
    public void janino(Blackhole blackhole) {
        generateInputs();
        i++;
        double x = fastEvaluator.apply(xValues);
        if (i == 100) {
            System.out.println("x = " + x);
        }
        blackhole.consume(x);
    }

    /**
     * This represents the "Speed of Light". How fast a human-written loop would
     * perform the SAME 1000 operations.
     *
     * @param blackhole
     */
    @Benchmark
    public void nativeJava(Blackhole blackhole) {
        generateInputs();
        double x = xValues[0];
        double y = xValues[1];

        double res = x;
        for (int i = 0; i < DEPTH; i++) {
            if (i % 2 == 0) {
                res += y;
            } else {
                res = Math.sin(res);
            }
        }
        blackhole.consume(res);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(StressTestExprLengthBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    public static String generateDeepExpression(int depth) {
        String base1 = "x+sin(x)+y+cos(y)";
        String base2 = "x-cos(x)-y+sin(y)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth / 2; i++) {
            if (i % 2 == 0) {
                sb.append("+").append(base1);
            } else {
                sb.append("-").append(base2);
            }
        }
        return sb.toString();
    }

    public static String generateDeepExpression1(int depth) {
        String base1 = "x+3*y";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth / 2; i++) {
            sb.append("+").append(base1);
        }
        return sb.toString();
    }
}
