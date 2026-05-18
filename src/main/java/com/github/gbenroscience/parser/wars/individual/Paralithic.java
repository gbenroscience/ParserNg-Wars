package com.github.gbenroscience.parser.wars.individual;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
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
     
    }

    

    // === Janino Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void paralithic(Blackhole blackhole) {
          generateInputs();
          blackhole.consume(this.expression.evaluate(xValues));
    } 
  
}
