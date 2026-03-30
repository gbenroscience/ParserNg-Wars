package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.objecthunter.exp4j.Expression;
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
 * JMH Benchmark comparing ParserNG, Exp4J, and JavaMEP. Focus: repeated
 * evaluation of the same pre-compiled expression.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 5)
@Fork(value = 2, warmups = 1)
@Threads(1)
public class TrigXBenchmarks {

    public static interface JaninoMathFunction {

        double apply(double x);
    }

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//

    static final int trigTerms = 30;
    private static final String EXPRESSION_SIN = sinMaclaurinSeries(trigTerms);
    private static final String EXPRESSION_COS = cosMaclaurinSeries(trigTerms);
    private static final String EXPRESSION = EXPRESSION_COS;
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private JaninoMathFunction fastEvaluator;

    int dataLen = 0;
    private int xSlot;

    double[] turboArgs = new double[11];

    static {
        System.out.println("expression:\n" + EXPRESSION);
    }

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        parserNG = new MathExpression(EXPRESSION, true);
        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        this.xSlot = this.parserNG.hasVariable("x") ? this.parserNG.getVariable("x").getFrameIndex() : -1;

        // 1. Convert your Math string to a Java-compatible String (e.g., "x + Math.sin(x)")
        String javaExpression = MathToJaninoConverter.convert(EXPRESSION);

        // 2. Build the Class Body
        // Notice: eval(double x) now actually performs the math calculation
        String classBody = String.format("""
         public double factorial(int n) {
                                           double result = 1;
                                           for (int i = 2; i <= n; i++) {
                                             result *= i;
                                           }
                                           return result;
                                         }                                
          public double eval(double x) {
              return %s;
          }
          
          @Override
          public double apply(double x) {
              return eval(x);
          }
          """, javaExpression);

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

        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        dataLen = randomData.length;

    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        parserNG.updateSlot(xSlot, randomData[cursor.getAndIncrement() % dataLen]);
        double result = parserNG.solveGeneric().scalar;
        blackhole.consume(result);
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        if (xSlot >= 0) {
            turboArgs[xSlot] = randomData[cursor.getAndIncrement() % dataLen];
        }
        double result = arrayBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        if (xSlot >= 0) {
            turboArgs[xSlot] = randomData[cursor.getAndIncrement() % dataLen];
        }
        double result = wideningBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    // === Janino Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void janino(Blackhole blackhole) {
        double x = randomData[cursor.getAndIncrement() % dataLen];
        double result = fastEvaluator.apply(x);
        blackhole.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TrigXBenchmarks.class.getSimpleName())
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

    public static String sinMaclaurinSeries(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int k = 2 * i + 1;
            String term = (i == 0 ? "" : (i % 2 == 0 ? " + " : " - "))
                    + "(x^" + k + ")/" + k + "!";
            sb.append(term);
        }
        return sb.toString();
    }

    public static String cosMaclaurinSeries(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int k = 2 * i;
            String term = (i == 0 ? "" : (i % 2 == 0 ? " + " : " - "))
                    + "(x^" + k + ")/" + k + "!";
            sb.append(term);
        }
        return sb.toString();
    }

}
