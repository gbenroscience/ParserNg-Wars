package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
 

/**
 * Build with:
 * mvn clean verify -U
 * Run with:
 * java -jar target/benchmarks.jar ".*FieryJanino.*" 
 * @author GBEMIRO
 */
public class FieryJanino extends ParserNGWars{

    public static interface JaninoMathFunction {

        double apply(double x[]);
    }
    
 
    // Pre-compiled instances (initialized in @Setup) 
    private ParserNGWars.JaninoMathFunction fastEvaluator;

    @Setup(Level.Trial)
    public void setup() {
        initRandomData();  
        setupJanino();
    }

 

    // === Janino(Fiery version) Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void janino(Blackhole blackhole) {
        generateInputs();
        double result = fastEvaluator.apply(xValues);
        blackhole.consume(result);
    } 


    private void setupJanino() {
        // Convert ParserNG syntax to Java syntax
        String javaExpr = MathToJaninoConverter.convert(EXPRESSION);
        int i = 0;
        for (String s : expressionVars) {
            javaExpr = javaExpr.replace(s, "v[" + i + "]");
            i++;
        }

        String classBody = String.format("""
            @Override
            public double apply(double[] v) {
                return %s;
            }
            """, javaExpr);
        //System.out.println("Janino-Expr = " + javaExpr);

        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{ParserNGWars.JaninoMathFunction.class});
            cbe.cook(classBody);
            this.fastEvaluator = (ParserNGWars.JaninoMathFunction) cbe.getClazz()
                    .getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

   

}
