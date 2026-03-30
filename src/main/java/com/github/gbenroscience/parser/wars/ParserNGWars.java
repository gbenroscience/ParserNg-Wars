package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import java.lang.reflect.InvocationTargetException;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.License;
import org.openjdk.jmh.runner.options.TimeValue;
import parsii.eval.Parser;
import parsii.eval.Variable;

/**
 * JMH Benchmark comparing ParserNG, Exp4J, and JavaMEP. Focus: repeated
 * evaluation of the same pre-compiled expression.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, warmups = 1)
@Threads(1)
public class ParserNGWars {

    public static final String[] getVars(String e) {
        return new MathExpression(e).getVariablesNames();
    }

    public static interface JaninoMathFunction {
        double apply(double x[]);
    }

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//
    // The expression to benchmark
    public static final String[] EXPRESSIONS = {
        "(sin(3) + cos(4 - sin(2))) ^ (-2)",
        "sin(3)+cos(5)-2.718281828459045^2",
        "((12+5)*3 - 2^3-13/12.23)^3.2",
        "5*sin(3+2)/(4*3-2)",
        "(1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8",
        "(sin(8+cos(3)) + 2 + ((27-5)/(8^3) * (3.14159 * 4^(14-10)) + sin(-3.141) + (0%4)) * 4/3 * 3/sqrt(4))+12",
        "((x^2 + sin(x)) / (1 + cos(x^2))) * (exp(x) / 10)",
        "((x^2 + 3*sin(x+5^3-1/4)) / (23/33 + cos(x^2))) * (exp(x) / 10)",
        "exp(5*4*3*2*1)",
        "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20",
        "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20+sin(x)",
        "2+3*4-5/2+sin(0)+cos(0)+sqrt(16)",
        "sin(7*x+y)+cos(7*x-y)-sin(4)+cos(5^6)",
        "((x^2 + 3*sin(x+5^3-1/4)) / (23/33 + cos(x^2))) * (exp(x) / 10) + (sin(3) + cos(4 - sin(2))) ^ (-2)",
        "(x^2+y^0.5)^4.2",
        "sin(x^3+y^3)-4*(x-y)",
        "(x+y+z)^0+(x+y+z)^1+(x+y+z)^2+(x+y+z)^3+(x+y+z)^4+(x+y+z)^5+(x+y+z)^6+(x+y+z)^7",
        "((x^2 + 3*sin(x+5^3-1/4+5*y)) / (23/33 + cos(x^2))) * (exp(x+2*z^2) / 10)",
        "sin(x)+3*cos(x)-4*x^2-8*x^3+9/(x+1)+5*(x-1)^3+12*y",
        "sin((x+y+z)^3.14)",
        "x+y+z",
        "x+y+z+sin(2)-cos(4)+exp(2^5)",
        "(x+y+z)/(x-y+z)",
        "sin((x+y+z)/(x-y+z))^3.14159265357",
        "sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)",
        "sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)+sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)",
        "cos(v+w-5*x-y-2*z)+sin(2*v+4*w-5*x-y-2*z)",
        "cos(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)+sin(2*v+4*w-5*x^2-3*y-2*z)+sin(x+y-v)+cos(p+q+r)+12*s",
        "sin(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)+sin(2)-cos(3)+tan(1.5)-sinh(4.22)+cos(4.15)",
        "(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)",
        "(x^2/sin(2*3.14159265357/y))-x/2",
        "(cos(1+exp(x1))/sqrt(sin(x1)^2-cos(x1)^2))+atan(x1)",//Janino loses
        "x1^3+x2^3+x3^3+x4^3",
        "x1^3.21+x2^3.14+x3^3+x4^3+x5^3+x6^3",//Janino wins
        "(sin(x1^3)-cos(x1^4)+tan(x1^0.5))/(2*x1^2+1)",
        "(sin(x1) + 2 + ((7-5) * (3.14159 * x1^(14-10)) + sin(-3.141) + (0%x1)) * x1/3 * 3/sqrt(x1+12))",//Janino loses
        "x1^3+x2^3+x3^3+x4^3+x5^3+x6^3"
    };

    private static final String EXPRESSION = EXPRESSIONS[EXPRESSIONS.length - 1];

    private static final String[] expressionVars = getVars(EXPRESSION);

    private static final int NUM_VARS = expressionVars.length;
    private double[] xValues = new double[NUM_VARS];
    private Object janinoArgs[] = new Object[NUM_VARS];
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private Expression exp4j;
    private JaninoMathFunction fastEvaluator;
    private ExpressionEvaluator expressEvaluator;
    private parsii.eval.Expression express;

    private org.mariuszgromada.math.mxparser.Expression mxParser;

    Variable pp;
    Variable pq;
    Variable pr;
    Variable ps;
    Variable pt;
    Variable pu;
    Variable pv;
    Variable pw;
    Variable px;
    Variable py;
    Variable pz;

    Argument p;
    Argument q;
    Argument r;
    Argument s;
    Argument t;
    Argument u;
    Argument v;
    Argument w;
    Argument x;
    Argument y;
    Argument z;

    double[] turboArgs = new double[11];
    private int[] slots = new int[NUM_VARS];

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        parserNG = new MathExpression(EXPRESSION, true);

        // Cache slot indices once
        for (int i = 0; i < NUM_VARS; i++) {
            slots[i] = parserNG.getVariable("x" + (i + 1)).getFrameIndex();
        }

        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        // Exp4J - build once
        exp4j = new ExpressionBuilder(EXPRESSION).variable("p").variable("q").variable("r").variable("s").variable("t").variable("u").
                variable("v").variable("w").variable("x").variable("y").variable("z").build();

        try {
            setupJanino();
            setupNormalJanino();
            parsii.eval.Scope scope = new parsii.eval.Scope();
            pp = scope.create("p");
            pq = scope.create("q");
            pr = scope.create("r");
            ps = scope.create("s");
            pt = scope.create("t");
            pu = scope.create("u");
            pv = scope.create("v");
            pw = scope.create("w");
            px = scope.create("x");
            py = scope.create("y");
            pz = scope.create("z");

            express = Parser.parse(EXPRESSION, scope);

            mxParser = new org.mariuszgromada.math.mxparser.Expression("u^3+v^3+w^3+x^3+y^3+z^3");
            p = new Argument("p", 1.0);
            q = new Argument("q", 1.0);
            r = new Argument("r", 1.0);
            s = new Argument("s", 1.0);
            t = new Argument("t", 1.0);
            u = new Argument("u", 1.0);
            v = new Argument("v", 1.0);
            w = new Argument("w", 1.0);
            x = new Argument("x", 1.0);
            y = new Argument("y", 1.0);
            z = new Argument("z", 1.0);
            License.iConfirmNonCommercialUse("JIBOYE");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, "Janino ClassBody Setup Failed", ex);
        }
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            parserNG.updateSlot(slots[i], xValues[i]);
        }
        double result = parserNG.solveGeneric().scalar;
        blackhole.consume(result);
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {
        generateInputs();
        /*   for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }
         */
        double result = arrayBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();
        /*for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }*/
        double result = wideningBasedTurbo.applyScalar(xValues);
        blackhole.consume(result);
    }

    // === Exp4J Benchmark ===
    /*  @org.openjdk.jmh.annotations.Benchmark
    public void exp4j(Blackhole blackhole) {
        exp4j.setVariable("x", randomData[cursor.getAndIncrement() % dataLen]);
        exp4j.setVariable("y", randomData[cursor.getAndIncrement() % dataLen] - 0.5);
        double result = exp4j.evaluate();
        blackhole.consume(result);
    }
     */
    // === Janino Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void janino(Blackhole blackhole) {
        generateInputs();
        double result = fastEvaluator.apply(xValues);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void normalJanino(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < xValues.length; i++) {
            janinoArgs[i] = xValues[i];
        }
        try {
            blackhole.consume(expressEvaluator.evaluate(janinoArgs));
        } catch (InvocationTargetException ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues[0]);
    }

    /*
    // === mXparser Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void mXparser(Blackhole blackhole) {
        int dataLen = 0;
        p.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] + 2.0);
        q.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] - 3.1);
        r.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] + 2.34);
        s.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] - 1.38);
        t.setArgumentValue(randomData[cursor.getAndIncrement() % dataLen] + 3.9);
        u.setArgumentValue(randomData[cursor.getAndIncrement() % dataLen]
                - 4.22);
        v.setArgumentValue(randomData[cursor.getAndIncrement() % dataLen]
                + 1.2);
        w.setArgumentValue(randomData[cursor.getAndIncrement() % dataLen]
                - 1.25);
        x.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen]);
        y.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] - 0.5);
        z.setArgumentValue(randomData[cursor.getAndIncrement()
                % dataLen] - 0.25);

        mxParser.addArguments(p, q, r, s, t, u, v, w, x, y, z);

        double result = mxParser.calculate();
        blackhole.consume(result);
    }
*/
    /*
    @org.openjdk.jmh.annotations.Benchmark
    public void parsii(Blackhole blackhole) {
        if (pp != null) {
            pp.setValue(randomData[cursor.getAndIncrement() % dataLen] + 2.0);
        }
        if (pq != null) {
            pq.setValue(randomData[cursor.getAndIncrement() % dataLen] - 3.1);
        }
        if (pr != null) {
            pr.setValue(randomData[cursor.getAndIncrement() % dataLen] + 2.34);
        }
        if (ps != null) {
            ps.setValue(randomData[cursor.getAndIncrement() % dataLen] - 1.38);
        }
        if (pt != null) {
            pt.setValue(randomData[cursor.getAndIncrement() % dataLen] + 3.9);
        }
        if (pu != null) {
            pu.setValue(randomData[cursor.getAndIncrement() % dataLen] - 4.22);
        }
        if (pv != null) {
            pv.setValue(randomData[cursor.getAndIncrement() % dataLen] + 1.2);
        }
        if (pw != null) {
            pw.setValue(randomData[cursor.getAndIncrement() % dataLen] - 1.25);
        }
        if (px != null) {
            px.setValue(randomData[cursor.getAndIncrement() % dataLen]);
        }
        if (py != null) {
            py.setValue(randomData[cursor.getAndIncrement() % dataLen] - 0.5);
        }
        if (pz != null) {
            pz.setValue(randomData[cursor.getAndIncrement() % dataLen] - 0.25);
        }
        double result = express.evaluate();
        blackhole.consume(result);
    }
     */

 /*
    // === JavaMEP Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void javaMep(Blackhole blackhole) {
        double result = Parser.simpleEval(EXPRESSION); // adjust method name to actual eval call
        blackhole.consume(result);
    }
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ParserNGWars.class.getSimpleName())
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .warmupIterations(5)
                .warmupTime(TimeValue.milliseconds(200L))
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(500))
                .forks(2)
                .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
                .jvmArgs("-Xms2g", "-Xmx2g") // tune heap if needed
                .build();

        new Runner(opt).run();
    }

    private void generateInputs() {
        double base = randomData[cursor.getAndIncrement() % randomData.length];
        xValues[0] = base;
        for (int i = 1; i < NUM_VARS; i++) {
            xValues[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
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
        System.out.println("Janino-Expr = " + javaExpr);

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
