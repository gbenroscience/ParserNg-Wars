package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
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
public class Parsii extends ParserNGWars{

   
    // Pre-compiled instances (initialized in @Setup) 
    private parsii.eval.Expression express;
    parsii.eval.Variable[] parsiiVars = new parsii.eval.Variable[expressionVars.length];
  

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        MathExpression.setAutoInitOn(true);
 

        parsii.eval.Scope scope = new parsii.eval.Scope();
        for (int i = 0; i < parsiiVars.length; i++) {
            parsiiVars[i] = scope.create(expressionVars[i]);
        }

        try {
            express = Parser.parse(EXPRESSION, scope);
        } catch (ParseException ex) {
            System.getLogger(Parsii.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    } 
        // === Parsii Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parsii(Blackhole blackhole) {
        generateInputs();
        int i = 0;
        for (Variable v : parsiiVars) {
            v.setValue(xValues[i++]);
        }
        double result = express.evaluate();
        blackhole.consume(result);
    }
 
 

}
