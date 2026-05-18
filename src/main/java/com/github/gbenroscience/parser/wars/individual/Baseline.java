package com.github.gbenroscience.parser.wars.individual;

import org.openjdk.jmh.annotations.Benchmark;
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
public class Baseline extends ParserNGWars{
 

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        
    }

    

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues.length == 0 ? 0.0 : xValues[0]);   
    } 
 
 

}
