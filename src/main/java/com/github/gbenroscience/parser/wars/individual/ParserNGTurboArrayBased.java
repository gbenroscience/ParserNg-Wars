package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

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
 

   // === ParserNG-Turob-Array-based Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs(); 
        double result = arrayBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    } 
 

}
