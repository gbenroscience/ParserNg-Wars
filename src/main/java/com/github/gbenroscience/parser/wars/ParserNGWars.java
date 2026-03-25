/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.wars.manual.Stats;
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
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
@Measurement(iterations = 5, time = 5)
@Fork(value = 2, warmups = 1)
@Threads(1)
public class ParserNGWars {

    public static interface JaninoMathFunction {

        double apply(double p, double q, double r, double s, double t, double u, double v, double w, double x, double y, double z);
    }

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//

    // The expression to benchmark
    private static final String EXPRESSION1 = "(sin(3) + cos(4 - sin(2))) ^ (-2)";
    private static final String EXPRESSION2 = "sin(3)+cos(5)-2.718281828459045^2";
    private static final String EXPRESSION3 = "((12+5)*3 - 2^3-13/12.23)^3.2";
    private static final String EXPRESSION4 = "5*sin(3+2)/(4*3-2)";
    private static final String EXPRESSION5 = "(1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8";

    private static final String EXPRESSION6 = "(sin(8+cos(3)) + 2 + ((27-5)/(8^3) * (3.14159 * 4^(14-10)) + sin(-3.141) + (0%4)) * 4/3 * 3/sqrt(4))+12";

    private static final String EXPRESSION7 = "((x^2 + sin(x)) / (1 + cos(x^2))) * (exp(x) / 10)";
    private static final String EXPRESSION8 = "((x^2 + 3*sin(x+5^3-1/4)) / (23/33 + cos(x^2))) * (exp(x) / 10)";
    private static final String EXPRESSION10 = "exp(5*4*3*2*1)";
    private static final String EXPRESSION11 = "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20";
    private static final String EXPRESSION12 = "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20+sin(x)";
    private static final String EXPRESSION13 = "2+3*4-5/2+sin(0)+cos(0)+sqrt(16)";

    private static final String EXPRESSION14 = "sin(7*x+y)+cos(7*x-y)-sin(4)+cos(5^6)";

    private static final String EXPRESSION15 = "((x^2 + 3*sin(x+5^3-1/4)) / (23/33 + cos(x^2))) * (exp(x) / 10) + (sin(3) + cos(4 - sin(2))) ^ (-2)";

    private static final String EXPRESSION16 = "(x^2+y^0.5)^4.2";

    private static final String EXPRESSION17 = "sin(x^3+y^3)-4*(x-y)";
    private static final String EXPRESSION18 = "(x+y+z)^0+(x+y+z)^1+(x+y+z)^2+(x+y+z)^3+(x+y+z)^4+(x+y+z)^5+(x+y+z)^6+(x+y+z)^7";
    private static final String EXPRESSION19 = "((x^2 + 3*sin(x+5^3-1/4+5*y)) / (23/33 + cos(x^2))) * (exp(x+2*z^2) / 10)";
    private static final String EXPRESSION20 = "sin(x)+3*cos(x)-4*x^2-8*x^3+9/(x+1)+5*(x-1)^3+12*y";
    private static final String EXPRESSION21 = "sin((x+y+z)^3.14)";
    private static final String EXPRESSION22 = "x+y+z";
    private static final String EXPRESSION23 = "x+y+z+sin(2)-cos(4)+exp(2^5)";
    private static final String EXPRESSION24 = "(x+y+z)/(x-y+z)";
    private static final String EXPRESSION25 = "sin((x+y+z)/(x-y+z))^3.14159265357";
    private static final String EXPRESSION26 = "sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)";
    private static final String EXPRESSION27 = "sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)+sin(x)+sin(y)+sin(z)-sin(x+1)-sin(x-1.1)-sin(y-1)-sin(y-1.1)+sin(z+1)+sin(z+2)+sin(z+3*x*y*z)";
    private static final String EXPRESSION28 = "cos(v+w-5*x-y-2*z)+sin(2*v+4*w-5*x-y-2*z)";
    private static final String EXPRESSION29 = "cos(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)+sin(2*v+4*w-5*x^2-3*y-2*z)+sin(x+y-v)+cos(p+q+r)+12*s";
    private static final String EXPRESSION30 = "sin(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)+sin(2)-cos(3)+tan(1.5)-sinh(4.22)+cos(4.15)";
    private static final String EXPRESSION31 = "(12*p+3*q-4*r+5*s-t-4*u+2*v+w-5*x-y-2*z)";
    private static final String EXPRESSION32 = "(x^2/sin(2*3.14159265357/y))-x/2";//Turbo is shockingly powerful at this! almost 2.5x faster than Janino
    private static final String EXPRESSION33 = "(cos(1+exp(x))/sqrt(sin(x)^2-cos(x)^2))+atan(x)";
    private static final String EXPRESSION34 = "w^3+x^3+y^3+z^3";
    private static final String EXPRESSION35 = "u^3.21+v^3.14+w^3+x^3+y^3+z^3";
    private static final String EXPRESSION36 = "(sin(x^3)-cos(x^4)+tan(x^0.5))/(2*x^2+1)";
    private static final String EXPRESSION37 = "(sin(x) + 2 + ((7-5) * (3.14159 * x^(14-10)) + sin(-3.141) + (0%x)) * x/3 * 3/sqrt(x+12))";
    private static final String EXPRESSION38 = "u^3+v^3+w^3+x^3+y^3+z^3";

    private static final String EXPRESSION = EXPRESSION15;
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private Expression exp4j;
    private JaninoMathFunction fastEvaluator;
    private parsii.eval.Expression express;

    int dataLen = 0;
    private int pSlot;
    private int qSlot;
    private int rSlot;
    private int sSlot;
    private int tSlot;
    private int uSlot;
    private int vSlot;
    private int wSlot;
    private int xSlot;
    private int ySlot;
    private int zSlot;

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

    double[] turboArgs = new double[11];

    @Setup(Level.Trial)
    public void setup() {
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        parserNG = new MathExpression(EXPRESSION, true);
        try {
            arrayBasedTurbo = new ScalarTurboEvaluator(parserNG, false).compile();
            wideningBasedTurbo = new ScalarTurboEvaluator(parserNG, true).compile();
        } catch (Throwable ex) {
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        this.pSlot = this.parserNG.hasVariable("p") ? this.parserNG.getVariable("p").getFrameIndex() : -1;
        this.qSlot = this.parserNG.hasVariable("q") ? this.parserNG.getVariable("q").getFrameIndex() : -1;
        this.rSlot = this.parserNG.hasVariable("r") ? this.parserNG.getVariable("r").getFrameIndex() : -1;
        this.sSlot = this.parserNG.hasVariable("s") ? this.parserNG.getVariable("s").getFrameIndex() : -1;
        this.tSlot = this.parserNG.hasVariable("t") ? this.parserNG.getVariable("t").getFrameIndex() : -1;
        this.uSlot = this.parserNG.hasVariable("u") ? this.parserNG.getVariable("u").getFrameIndex() : -1;
        this.vSlot = this.parserNG.hasVariable("v") ? this.parserNG.getVariable("v").getFrameIndex() : -1;
        this.wSlot = this.parserNG.hasVariable("w") ? this.parserNG.getVariable("w").getFrameIndex() : -1;
        this.xSlot = this.parserNG.hasVariable("x") ? this.parserNG.getVariable("x").getFrameIndex() : -1;
        this.ySlot = this.parserNG.hasVariable("y") ? this.parserNG.getVariable("y").getFrameIndex() : -1;
        this.zSlot = this.parserNG.hasVariable("z") ? this.parserNG.getVariable("z").getFrameIndex() : -1;
        // Exp4J - build once
        exp4j = new ExpressionBuilder(EXPRESSION).variable("p").variable("q").variable("r").variable("s").variable("t").variable("u").
                variable("v").variable("w").variable("x").variable("y").variable("z").build();

//        try {
//            this.fastEvaluator = (JaninoMathFunction) org.codehaus.janino.ExpressionEvaluator.createFastExpressionEvaluator(
//                    MathToJaninoConverter.convert(EXPRESSION), // The expression
//                    JaninoMathFunction.class, // Our single-method interface
//                    new String[]{"x", "y", "z"}, // Parameter name
//                    (ClassLoader) null // Use current thread's classloader
//            );
//        } catch (CompileException ex) {
//            System.getLogger(Benchmark.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
//        }
        try {
            org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();

            // 1. Tell Janino which interface we are implementing
            cbe.setImplementedInterfaces(new Class[]{JaninoMathFunction.class});

            // 2. Build the actual Java class body as a string
            // This removes all ambiguity about method names or parameters
            String classBody
                    = "public double apply(double p, double q, double r, double s, double t, double u, double v, double w, double x, double y, double z) {\n"
                    + "    return " + MathToJaninoConverter.convert(EXPRESSION) + ";\n"
                    + "}";

            cbe.cook(classBody);

            // 3. Instantiate the generated class
            this.fastEvaluator = (JaninoMathFunction) cbe.getClazz().getDeclaredConstructor().newInstance();

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

        } catch (Exception ex) {
            ex.printStackTrace();
            System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, "Janino ClassBody Setup Failed", ex);
        }

        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        dataLen = randomData.length;

    }

    // === ParserNG Benchmark ===
    /* @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        parserNG.updateSlot(xSlot, randomData[cursor.getAndIncrement() % dataLen]);
        parserNG.updateSlot(ySlot, randomData[cursor.getAndIncrement() % dataLen] - 0.5);
        double result = parserNG.solveGeneric().scalar;
        blackhole.consume(result);
    }*/
    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboArrayBased(Blackhole blackhole) {

        if (pSlot >= 0) {
            turboArgs[pSlot] = randomData[cursor.getAndIncrement() % dataLen] + 2.0;
        }
        if (qSlot >= 0) {
            turboArgs[qSlot] = randomData[cursor.getAndIncrement() % dataLen] - 3.1;
        }
        if (rSlot >= 0) {
            turboArgs[rSlot] = randomData[cursor.getAndIncrement() % dataLen] + 2.34;
        }
        if (sSlot >= 0) {
            turboArgs[sSlot] = randomData[cursor.getAndIncrement() % dataLen] - 1.38;
        }
        if (tSlot >= 0) {
            turboArgs[tSlot] = randomData[cursor.getAndIncrement() % dataLen] + 3.9;
        }
        if (uSlot >= 0) {
            turboArgs[uSlot] = randomData[cursor.getAndIncrement() % dataLen] - 4.22;
        }
        if (vSlot >= 0) {
            turboArgs[vSlot] = randomData[cursor.getAndIncrement() % dataLen] + 1.2;
        }

        if (wSlot >= 0) {
            turboArgs[wSlot] = randomData[cursor.getAndIncrement() % dataLen] - 1.25;
        }
        if (xSlot >= 0) {
            turboArgs[xSlot] = randomData[cursor.getAndIncrement() % dataLen];
        }
        if (ySlot >= 0) {
            turboArgs[ySlot] = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
        }
        if (zSlot >= 0) {
            turboArgs[zSlot] = randomData[cursor.getAndIncrement() % dataLen] - 0.25;
        }

        double result = arrayBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {

        if (pSlot >= 0) {
            turboArgs[pSlot] = randomData[cursor.getAndIncrement() % dataLen] + 2.0;
        }
        if (qSlot >= 0) {
            turboArgs[qSlot] = randomData[cursor.getAndIncrement() % dataLen] - 3.1;
        }
        if (rSlot >= 0) {
            turboArgs[rSlot] = randomData[cursor.getAndIncrement() % dataLen] + 2.34;
        }
        if (sSlot >= 0) {
            turboArgs[sSlot] = randomData[cursor.getAndIncrement() % dataLen] - 1.38;
        }
        if (tSlot >= 0) {
            turboArgs[tSlot] = randomData[cursor.getAndIncrement() % dataLen] + 3.9;
        }
        if (uSlot >= 0) {
            turboArgs[uSlot] = randomData[cursor.getAndIncrement() % dataLen] - 4.22;
        }
        if (vSlot >= 0) {
            turboArgs[vSlot] = randomData[cursor.getAndIncrement() % dataLen] + 1.2;
        }

        if (wSlot >= 0) {
            turboArgs[wSlot] = randomData[cursor.getAndIncrement() % dataLen] - 1.25;
        }
        if (xSlot >= 0) {
            turboArgs[xSlot] = randomData[cursor.getAndIncrement() % dataLen];
        }
        if (ySlot >= 0) {
            turboArgs[ySlot] = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
        }
        if (zSlot >= 0) {
            turboArgs[zSlot] = randomData[cursor.getAndIncrement() % dataLen] - 0.25;
        }

        double result = wideningBasedTurbo.applyScalar(turboArgs);
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
        double p = randomData[cursor.getAndIncrement() % dataLen] + 2.0;
        double q = randomData[cursor.getAndIncrement() % dataLen] - 3.1;
        double r = randomData[cursor.getAndIncrement() % dataLen] + 2.34;
        double s = randomData[cursor.getAndIncrement() % dataLen] - 1.38;
        double t = randomData[cursor.getAndIncrement() % dataLen] + 3.9;
        double u = randomData[cursor.getAndIncrement() % dataLen] - 4.22;
        double v = randomData[cursor.getAndIncrement() % dataLen] + 1.2;
        double w = randomData[cursor.getAndIncrement() % dataLen] - 1.25;
        double x = randomData[cursor.getAndIncrement() % dataLen];
        double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
        double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;
        double result = fastEvaluator.apply(p, q, r, s, t, u, v, w, x, y, z);
        blackhole.consume(result);
    }
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
                .measurementTime(TimeValue.milliseconds(1000))
                .forks(2)
                .addProfiler(org.openjdk.jmh.profile.GCProfiler.class)
                .jvmArgs("-Xms2g", "-Xmx2g") // tune heap if needed
                .build();

        new Runner(opt).run();
    }

    private static String generateDeep(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(i % 2 == 0 ? "sin(" : "cos(");
        }
        sb.append("x");
        for (int i = 0; i < depth; i++) {
            sb.append(")");
        }
        return sb.toString();
    }
}
