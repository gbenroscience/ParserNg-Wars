package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Build with: mvn clean verify -U Run with: java -jar target/benchmarks.jar
 * ".*FieryJanino.*"
 *
 * @author GBEMIRO
 */
public class FieryJanino extends ParserNGWars {

    public static interface JaninoMathFunction {

        double apply(double x[]);
    }

    // Pre-compiled instances (initialized in @Setup) 
    private ParserNGWars.JaninoMathFunction fastEvaluator;

    @Setup(Level.Trial)
    public void setup() {
        super.setup();
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

        // Use an index loop to cleanly target array tracking positions
        for (int i = 0; i < expressionVars.length; i++) {
            String varName = expressionVars[i];
            // \b ensures we match exact variable tokens (e.g., matching "x1" but ignoring "x10")
            String regex = "\\b" + java.util.regex.Pattern.quote(varName) + "\\b";
            javaExpr = javaExpr.replaceAll(regex, "v[" + i + "]");
        }

        String classBody = String.format("""
        @Override
        public double apply(double[] v) {
            return %s;
        }
        """, javaExpr);

        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[]{ParserNGWars.JaninoMathFunction.class});
            cbe.cook(classBody);
            this.fastEvaluator = (ParserNGWars.JaninoMathFunction) cbe.getClazz()
                    .getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            // Rethrowing as RuntimeException prevents JMH from continuing silently with null states
            throw new RuntimeException("Failed to compile Janino function body expressions", ex);
        }
    }

}
