package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import com.github.gbenroscience.parser.wars.Stats;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.License;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import parsii.eval.Parser;
import parsii.eval.Variable;
import parsii.tokenizer.ParseException;

/**
 *
 * @author GBEMIRO
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, warmups = 1)
@Threads(1)
public class Parsii {

    private static final String[] getVars(String e) {
        return new MathExpression(e).getVariablesNames();
    }

    private int[] randomData;
    AtomicInteger cursor = new AtomicInteger();//
    // The expression to benchmark
    private static final String[] EXPRESSIONS = {
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
    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNG;
    private FastCompositeExpression arrayBasedTurbo;
    private FastCompositeExpression wideningBasedTurbo;
    private parsii.eval.Expression express;
    parsii.eval.Variable[] parsiiVars = new parsii.eval.Variable[expressionVars.length];
 

    double[] turboArgs = new double[NUM_VARS];
    private final int[] slots = new int[NUM_VARS];

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
            System.getLogger(Parsii.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        parsii.eval.Scope scope = new parsii.eval.Scope();
        for (int i = 0; i < parsiiVars.length; i++) {
            parsiiVars[i] = scope.create(expressionVars[i]);
        }

        try {
            express = Parser.parse(EXPRESSION, scope);
        } catch (ParseException ex) {
            System.getLogger(Parsii.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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
        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }
        double result = arrayBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void parserNgTurboWideningBased(Blackhole blackhole) {
        generateInputs();
        for (int i = 0; i < NUM_VARS; i++) {
            turboArgs[slots[i]] = xValues[i];
        }
        double result = wideningBasedTurbo.applyScalar(turboArgs);
        blackhole.consume(result);
    }

    @Benchmark
    public void baseline(Blackhole blackhole) {
        generateInputs(); // Measures just the overhead of creating the 30 variables
        blackhole.consume(xValues[0]);
    }

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
 
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Parsii.class.getSimpleName())
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

}
