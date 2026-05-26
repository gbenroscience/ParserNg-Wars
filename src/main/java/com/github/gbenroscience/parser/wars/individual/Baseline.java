package com.github.gbenroscience.parser.wars.individual;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*Exp4J.*" 
 * @author GBEMIRO
 */
public class Baseline extends ParserNGWars{
 
 

    
   // ===Cost of variable setup per iteration Benchmark ===
    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);   
    } 
 
 

}
