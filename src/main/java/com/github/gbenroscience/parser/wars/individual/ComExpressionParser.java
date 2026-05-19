package com.github.gbenroscience.parser.wars.individual;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

/**
 *
 * @author GBEMIRO
 */
public class ComExpressionParser extends ParserNGWars {

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
    }

    // ===exp4J Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void comExpressionParser(Blackhole blackhole) {
        generateDoubleInputs();  
        double result = com.expression.parser.Parser.eval(EXPRESSION, expressionVars, xValuesObj);
        blackhole.consume(result);
    }

}
