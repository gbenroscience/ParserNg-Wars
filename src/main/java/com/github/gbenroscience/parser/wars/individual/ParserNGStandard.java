package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression; 
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
public class ParserNGStandard extends ParserNGWars{
    // Pre-compiled instances (initialized in @Setup)  
  

    @Setup(Level.Trial)
    public void setup() {
        super.setup();
        // ParserNG - compile once 
        parserNG = new MathExpression(EXPRESSION, true);  
    }

    // === ParserNG Standard Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs(); 
        double result = parserNG.solveGeneric(xValues).scalar;
        blackhole.consume(result);
    }
 

  
 
 
 

}
