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
public class ParserNGTurboWideningBased extends ParserNGWars{

   
    // Pre-compiled instances (initialized in @Setup) 
    protected FastCompositeExpression wideningBasedTurbo; 
  

    @Setup(Level.Trial)
    public void setup() {
        super.setup();
        // ParserNG - compile once 
        parserNG = new MathExpression(EXPRESSION, true); 
        try {
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(ParserNGTurboWideningBased.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
 
 
    }
 
 
    // === ParserNG-Turob-Widening Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs(); 
        double result = wideningBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }
 
}
