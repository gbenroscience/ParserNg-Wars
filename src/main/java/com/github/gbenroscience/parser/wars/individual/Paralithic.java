package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.util.concurrent.TimeUnit;
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
import com.dfsek.paralithic.Expression;
import com.dfsek.paralithic.eval.parser.Parser;
import com.dfsek.paralithic.eval.tokenizer.ParseException;

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*Paralithic.*" 
 * @author GBEMIRO
 */
public class Paralithic extends ParserNGWars{
  
  
   
    
    // Pre-compiled instances (initialized in @Setup) 
    private Expression expression;

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        Parser parser = new Parser();
        com.dfsek.paralithic.eval.parser.Scope scope = new com.dfsek.paralithic.eval.parser.Scope();
        
        
        for (int i = 0; i < expressionVars.length; i++) {
            scope.addInvocationVariable(expressionVars[i]);
        }
        try {
            this.expression = parser.parse(EXPRESSION, scope);
        } catch (ParseException ex) {
            System.getLogger(Paralithic.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once
        parserNG = new MathExpression(EXPRESSION, true);

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(FieryJanino.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
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
        // turboArgs = xValues;
        double result = arrayBasedTurbo.applyScalar(xValues);//assume the xValues lines up with the turboArgs
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();
        // turboArgs = xValues;
        double result = wideningBasedTurbo.applyScalar(xValues);//assume the xValues lines up with the turboArgs
        blackhole.consume(result);
    }

    // === Janino Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void paralithic(Blackhole blackhole) {
          generateInputs();
          blackhole.consume(this.expression.evaluate(xValues));
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);
    }
 
   

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Paralithic.class.getSimpleName())
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
