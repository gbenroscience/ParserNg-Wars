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
import parsii.eval.Parser;
import parsii.eval.Variable;
import parsii.tokenizer.ParseException;

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*Parsii.*"
 * @author GBEMIRO
 */
public class ParserNGTurboArrayBased extends ParserNGWars{
 
    
    
    // Pre-compiled instances (initialized in @Setup) 
    protected FastCompositeExpression arrayBasedTurbo; 
  

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once
        parserNG = new MathExpression(EXPRESSION, true); 

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile(); 
        } catch (Throwable ex) {
            System.getLogger(ParserNGTurboArrayBased.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
 
    }
 

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs(); 
        double result = arrayBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    } 
 

}
