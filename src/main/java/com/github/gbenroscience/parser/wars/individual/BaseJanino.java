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
        initRandomData();
        setupNormalJanino();
    }

  
   // ===Janino(base) Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void normalJanino(Blackhole blackhole) {
        generateObjectInputs();
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
