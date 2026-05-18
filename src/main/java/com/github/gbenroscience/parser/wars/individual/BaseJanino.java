package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
 
/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*BaseJanino.*" 
 * @author GBEMIRO
 */
public class BaseJanino extends ParserNGWars{

    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
   // private FastCompositeExpression functionBasedTurbo;
    private ExpressionEvaluator expressEvaluator;
 
    private final Object janinoArgs[] = new Object[NUM_VARS];

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        parserNG = new MathExpression(EXPRESSION, true);
  
        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(BaseJanino.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } 
        setupNormalJanino();
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs(); 
        double result = parserNG.solveGeneric(xValues).scalar;
        blackhole.consume(result);
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs();
        double result = arrayBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }
 
//        // === ParserNG Benchmark ===
//    @org.openjdk.jmh.annotations.Benchmark
//    public void functionBasedTurbo(Blackhole blackhole) {
//        generateInputs();
//        double result = functionBasedTurbo.applyScalar(xValues);
//        blackhole.consume(result);
//    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs(); 
        double result = wideningBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void normalJanino(Blackhole blackhole) {
        generateObjectInputs();
        try {
            blackhole.consume(expressEvaluator.evaluate(janinoArgs));
        } catch (InvocationTargetException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);
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
