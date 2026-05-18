package com.github.gbenroscience.parser.wars.individual;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

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
       

        ExpressionBuilder builder = new ExpressionBuilder(EXPRESSION);
        for (int i = 0; i < NUM_VARS; i++) {
            builder = builder.variable(expressionVars[i]);
        }
        exp4j = builder.build();
    }
    
    
    // ===exp4J Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void exp4j(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            exp4j.setVariable(expressionVars[i], xValues[i]);
        }
        double result = exp4j.evaluate();
        blackhole.consume(result);
    }
 
 

}
