package com.github.gbenroscience.parser.wars.individual;

import com.expression.parser.util.ParserResult;
import com.expression.parser.util.Point;
import static com.github.gbenroscience.parser.wars.individual.ParserNGWars.NUM_VARS;
import static com.github.gbenroscience.parser.wars.individual.ParserNGWars.expressionVars;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

/**
 *
 * @author GBEMIRO
 */
public class ComExpressionParser extends ParserNGWars {
    Point[] sbesadaPts = new Point[expressionVars.length];
    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
    }

    @Override
    protected void generateInputs() {
         double base = randomData[simpleCursor++ % randomData.length];
        //double base = randomData[cursor.getAndIncrement() % randomData.length];
        if (sbesadaPts.length != 0) {
            sbesadaPts[0] = new Point(expressionVars[0], base);
        }
        for (int i = 1; i < NUM_VARS; i++) {
            sbesadaPts[i] = new Point(expressionVars[i], base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1));
        }
    }

    
    
    // ===exp4J Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void comExpressionParser(Blackhole blackhole) {
        generateInputs();
        ParserResult result = com.expression.parser.Parser.eval(EXPRESSION, sbesadaPts);
        blackhole.consume(result);
    }

}
