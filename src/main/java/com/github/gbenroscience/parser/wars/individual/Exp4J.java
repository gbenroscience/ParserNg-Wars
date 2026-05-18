package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.util.concurrent.TimeUnit;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
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
 * java -jar target/benchmarks.jar ".*Exp4J.*" 
 * @author GBEMIRO
 */
public class Exp4J extends ParserNGWars{

 
    // Pre-compiled instances (initialized in @Setup) 
    private Expression exp4j;

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
            System.getLogger(Exp4J.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        ExpressionBuilder builder = new ExpressionBuilder(EXPRESSION);
        for (int i = 0; i < NUM_VARS; i++) {
            builder = builder.variable(expressionVars[i]);
        }
        exp4j = builder.build();
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

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();
        double result = wideningBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);   
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void exp4j(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            exp4j.setVariable(expressionVars[i], xValues[i]);
        }
        double result = exp4j.evaluate();
        blackhole.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Exp4J.class.getSimpleName())
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
