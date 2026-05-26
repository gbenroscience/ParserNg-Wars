package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import static com.github.gbenroscience.parser.wars.individual.ParserNGWars.*;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
 
/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*BaseJanino.*" 
 * @author GBEMIRO
 */
public class BaseJanino extends ParserNGWars{

    // Pre-compiled instances (initialized in @Setup) 
    private ExpressionEvaluator expressEvaluator;
  

    @Setup(Level.Trial)
    public void setup() {
        super.setup();
        setupNormalJanino();
    }

    @Override
    protected void generateInputs() {
            double base = randomData[simpleCursor++ % randomData.length];
        //double base = randomData[cursor.getAndIncrement() % randomData.length];
        if (janinoArgs.length != 0) {
            janinoArgs[0] = base;
        }
        for (int i = 1; i < NUM_VARS; i++) {
            janinoArgs[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1);
        }
    }

    
    
  
   // ===Janino(base) Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void normalJanino(Blackhole blackhole) {
        generateInputs();
        try {
            blackhole.consume(expressEvaluator.evaluate(janinoArgs));
        } catch (InvocationTargetException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
 

    private void setupNormalJanino() {
        try {
            expressEvaluator = new ExpressionEvaluator();
            Class[] clazz = new Class[NUM_VARS];
            for (int i = 0; i < NUM_VARS; i++) {
                clazz[i] = double.class;
            }
            expressEvaluator.setParameters(expressionVars, clazz);
            expressEvaluator.setReturnType(double.class);
            expressEvaluator.cook(MathToJaninoConverter.convert(EXPRESSION));

        } catch (CompileException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

 

}
