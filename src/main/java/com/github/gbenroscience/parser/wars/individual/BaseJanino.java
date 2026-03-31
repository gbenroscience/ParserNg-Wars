package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import com.github.gbenroscience.parser.wars.ParserNGWars;
import com.github.gbenroscience.parser.wars.Stats;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
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
 *
 * @author GBEMIRO
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, warmups = 1)
@Threads(1)
public class BaseJanino {

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//
    // The expression to benchmark
    private static final String[] EXPRESSIONS = ParserNGWars.EXPRESSIONS;

    private static final String EXPRESSION = EXPRESSIONS[EXPRESSIONS.length - 3];

    private static final String[] expressionVars = ParserNGWars.getVars(EXPRESSION);

    private static final int NUM_VARS = expressionVars.length;
    private final double[] xValues = new double[NUM_VARS];
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private ExpressionEvaluator expressEvaluator;

    double[] turboArgs = new double[NUM_VARS];
    private final int[] slots = new int[NUM_VARS];
    private final Object janinoArgs[] = new Object[NUM_VARS];

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
            System.getLogger(BaseJanino.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        setupNormalJanino();

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
    
    
    @org.openjdk.jmh.annotations.Benchmark
    public void normalJanino(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < xValues.length; i++) {
            janinoArgs[i] = xValues[i];
        }
        try {
            blackhole.consume(expressEvaluator.evaluate(janinoArgs));
        } catch (InvocationTargetException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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

    private void setupNormalJanino() {
        try {
            expressEvaluator = new ExpressionEvaluator();
            Class[] clazz = new Class[NUM_VARS];
            for (int i = 0; i < NUM_VARS; i++) {
                clazz[i] = double.class;
            }
            expressEvaluator.setParameters(expressionVars, clazz);
            expressEvaluator.setReturnType(double.class);
            expressEvaluator.cook(MathToJaninoConverter.convert(EXPRESSION));

        } catch (CompileException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BaseJanino.class.getSimpleName())
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

}
