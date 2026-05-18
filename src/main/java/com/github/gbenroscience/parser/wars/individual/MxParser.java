package com.github.gbenroscience.parser.wars.individual;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.License;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;


/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*MxParser.*" 
 * @author GBEMIRO
 */
public class MxParser extends ParserNGWars{
 
 
    // Pre-compiled instances (initialized in @Setup) 
    private org.mariuszgromada.math.mxparser.Expression mxParser;
 
    private final Argument[]mXparserArgs= new Argument[NUM_VARS];

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();
        
        mxParser = new org.mariuszgromada.math.mxparser.Expression(EXPRESSION);

        for (int i = 0; i < NUM_VARS; i++) {
            mXparserArgs[i] = new Argument(expressionVars[i], 0.0);
        }
        mxParser.addArguments(mXparserArgs);
        License.iConfirmNonCommercialUse("JIBOYE");
    }
 
   // ===mXparser Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void mXparser(Blackhole blackhole) {
        generateInputs(); 
        for(int i=0;i<NUM_VARS;i++){
            mxParser.setArgumentValue(mXparserArgs[i].getArgumentName(), xValues[i]);
        }
        double result = mxParser.calculate();
        blackhole.consume(result);
    }
 

}
