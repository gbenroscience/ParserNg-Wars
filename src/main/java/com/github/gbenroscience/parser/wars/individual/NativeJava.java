package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression; 
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole; 
 

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*NativeJava.*" 
 * @author GBEMIRO
 */
public class NativeJava extends ParserNGWars{
 

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        MathExpression.setAutoInitOn(true); 
    } 
     // === Native Java Benchmark ===
       @Benchmark
    public void nativeJava(Blackhole bh) {
         generateInputs();
        double result = BenchmarkExpressions.STATEMENTS[index].apply(xValues);
        bh.consume(result);
    }
     
}
