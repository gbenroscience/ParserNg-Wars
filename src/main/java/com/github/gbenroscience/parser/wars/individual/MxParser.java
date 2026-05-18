package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.util.concurrent.TimeUnit;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.License;
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
 * java -jar target/benchmarks.jar ".*MxParser.*" 
 * @author GBEMIRO
 */
public class MxParser extends ParserNGWars{
 
 
    // Pre-compiled instances (initialized in @Setup) 
    private org.mariuszgromada.math.mxparser.Expression mxParser;
 
    private final Argument[]mXparserArgs= new Argument[NUM_VARS];

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
            System.getLogger(MxParser.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        mxParser = new org.mariuszgromada.math.mxparser.Expression(EXPRESSION);

        for (int i = 0; i < NUM_VARS; i++) {
            mXparserArgs[i] = new Argument(expressionVars[i], 0.0);
        }
        mxParser.addArguments(mXparserArgs);
        License.iConfirmNonCommercialUse("JIBOYE");
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
    public void mXparser(Blackhole blackhole) {
        generateInputs(); 
        for(int i=0;i<NUM_VARS;i++){
            mxParser.setArgumentValue(mXparserArgs[i].getArgumentName(), xValues[i]);
        }
        double result = mxParser.calculate();
        blackhole.consume(result);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MxParser.class.getSimpleName())
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
