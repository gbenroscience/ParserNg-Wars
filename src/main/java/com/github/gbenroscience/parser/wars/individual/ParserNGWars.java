package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.wars.MathToJaninoConverter;
import com.github.gbenroscience.parser.wars.Stats;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.function.ToDoubleFunction;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * JMH Benchmark comparing ParserNG, Exp4J, and JavaMEP. Focus: repeated
 * evaluation of the same pre-compiled expression.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, warmups = 1)
@Threads(1)
public class ParserNGWars {

    // Pre-compiled instances (initialized in @Setup)
    protected MathExpression parserNG;

    // The expression to benchmark
    public static final String[] EXPRESSIONS = {
        "(sin(3) + cos(4 - sin(2))) ^ (-2)",
        "sin(3)+cos(5)-2.718281828459045^2",
        "((12+5)*3 - 2^3-13/12.23)^3.2",
        "5*sin(3+2)/(4*3-2)",
        "(1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8",
        "(sin(8+cos(3)) + 2 + ((27-5)/(8^3) * (3.14159 * 4^(14-10)) + sin(-3.141) + (0%4)) * 4/3 * 3/sqrt(4))+12",
        "((x1^2 + sin(x1)) / (1 + cos(x1^2))) * (exp(x1) / 10)",
        "((x1^2 + 3*sin(x1+5^3-1/4)) / (23/33 + cos(x1^2))) * (exp(x1) / 10)",
        "exp(5*4*3*2*1)",
        "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20",
        "1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20+sin(x1)",
        "2+3*4-5/2+sin(0)+cos(0)+sqrt(16)",
        "sin(7*x1+x2)+cos(7*x1-x2)-sin(4)+cos(5^6)",
        "((x1^2 + 3*sin(x1+5^3-1/4)) / (23/33 + cos(x1^2))) * (exp(x1) / 10) + (sin(3) + cos(4 - sin(2))) ^ (-2)",
        "(x1^2+x2^0.5)^4.2",
        "sin(x1^3+x2^3)-4*(x1-x2)",
        "(x1+x2+x3)^0+(x1+x2+x3)^1+(x1+x2+x3)^2+(x1+x2+x3)^3+(x1+x2+x3)^4+(x1+x2+x3)^5+(x1+x2+x3)^6+(x1+x2+x3)^7",
        "((x1^2 + 3*sin(x1+5^3-1/4+5*x2)) / (23/33 + cos(x1^2))) * (exp(x1+2*x3^2) / 10)",
        "sin(x1)+3*cos(x1)-4*x1^2-8*x1^3+9/(x1+1)+5*(x1-1)^3+12*x2",
        "sin((x1+x2+x3)^3.14)",
        "x1+x2+x3",
        "x1+x2+x3+sin(2)-cos(4)+exp(2^5)",
        "(x1+x2+x3)/(x1-x2+x3)",
        "sin((x1+x2+x3)/(x1-x2+x3))^3.14159265357",
        "sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)",
        "sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)+sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)",
        "cos(x1+x2-5*x3-x4-2*x5)+sin(2*x1+4*x2-5*x3-x4-2*x5)",
        "cos(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)+sin(2*x7+4*x8-5*x9^2-3*x10-2*x11)+sin(x9+x10-x7)+cos(x1+x2+x3)+12*x4",
        "sin(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)+sin(2)-cos(3)+tan(1.5)-sinh(4.22)+cos(4.15)",
        "(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)",
        "(x1^2/sin(2*3.14159265357/x2))-x1/2",
        "(cos(1+exp(x1))/sqrt(sin(x1)^2-cos(x1)^2))+atan(x1)",
        "x1^3+x2^3+x3^3+x4^3",
        "x1^3.21+x2^3.14+x3^3+x4^3+x5^3+x6^3",
        "(sin(x1^3)-cos(x1^4)+tan(x1^0.5))/(2*x1^2+1)",
        "(sin(x1) + 2 + ((7-5) * (3.14159 * x1^(14-10)) + sin(-3.141) + (0%x1)) * x1/3 * 3/sqrt(x1+12))",
        "x1^3+x2^3+x3^3+x4^3+x5^3+x6^3",
        "sin(sqrt(x1^2+x2^2+x3^2))"
    };

    static int index = 23;//EXPRESSIONS.length - 3;

    public static final String[] getVars(String e) {
        return new MathExpression(e).getVariablesNames();
    }
    protected int simpleCursor;

    protected int[] randomData;

    protected AtomicInteger cursor = new AtomicInteger();//

    protected static final String EXPRESSION = ParserNGWars.getExpression();
    protected static final String[] expressionVars = ParserNGWars.getVars(EXPRESSION);

    protected static final int NUM_VARS = expressionVars.length;
    protected final double[] xValues = new double[NUM_VARS];
    protected final Object[] janinoArgs = new Object[NUM_VARS];

    static {
        System.out.println("EXPRESSION: " + EXPRESSION);
    }

    public static interface JaninoMathFunction {

        double apply(double x[]);
    }

    protected void generateInputs() {
        double base = randomData[simpleCursor++ % randomData.length];
        //double base = randomData[cursor.getAndIncrement() % randomData.length];
        if (xValues.length != 0) {
            xValues[0] = base;
        }
        for (int i = 1; i < NUM_VARS; i++) {
            xValues[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
    }

    protected void generateObjectInputs() {
        double base = randomData[simpleCursor++ % randomData.length];
        //double base = randomData[cursor.getAndIncrement() % randomData.length];
        if (janinoArgs.length != 0) {
            janinoArgs[0] = base;
        }
        for (int i = 1; i < NUM_VARS; i++) {
            janinoArgs[i] = base + (i % 2 == 0 ? 1.0 : -1.0) * (0.1 + (i % 10) * 0.1); // your original pattern
        }
        // You can fine-tune the offsets to better match your original values if needed
    }

    protected void initRandomData() {
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
    }

    public static final String getExpression() {
        return EXPRESSIONS[index];
    }

    public static final String EXPRESSIONS_MAP
            = """
            0====(sin(3) + cos(4 - sin(2))) ^ (-2)
            1====sin(3)+cos(5)-2.718281828459045^2
            2====((12+5)*3 - 2^3-13/12.23)^3.2
            3====5*sin(3+2)/(4*3-2)
            4====(1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8
            5====(sin(8+cos(3)) + 2 + ((27-5)/(8^3) * (3.14159 * 4^(14-10)) + sin(-3.141) + (0%4)) * 4/3 * 3/sqrt(4))+12
            6====((x1^2 + sin(x1)) / (1 + cos(x1^2))) * (exp(x1) / 10)
            7====((x1^2 + 3*sin(x1+5^3-1/4)) / (23/33 + cos(x1^2))) * (exp(x1) / 10)
            8====exp(5*4*3*2*1)
            9====1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20
            10====1+2+3+4+5+6+7+8+9+10+11+12+13+14+15+16+17+18+19+20+sin(x1)
            11====2+3*4-5/2+sin(0)+cos(0)+sqrt(16)
            12====sin(7*x1+x2)+cos(7*x1-x2)-sin(4)+cos(5^6)
            13====((x1^2 + 3*sin(x1+5^3-1/4)) / (23/33 + cos(x1^2))) * (exp(x1) / 10) + (sin(3) + cos(4 - sin(2))) ^ (-2)
            14====(x1^2+x2^0.5)^4.2
            15====sin(x1^3+x2^3)-4*(x1-x2)
            16====(x1+x2+x3)^0+(x1+x2+x3)^1+(x1+x2+x3)^2+(x1+x2+x3)^3+(x1+x2+x3)^4+(x1+x2+x3)^5+(x1+x2+x3)^6+(x1+x2+x3)^7
            17====((x1^2 + 3*sin(x1+5^3-1/4+5*x2)) / (23/33 + cos(x1^2))) * (exp(x1+2*x3^2) / 10)
            18====sin(x1)+3*cos(x1)-4*x1^2-8*x1^3+9/(x1+1)+5*(x1-1)^3+12*x2
            19====sin((x1+x2+x3)^3.14)
            20====x1+x2+x3
            21====x1+x2+x3+sin(2)-cos(4)+exp(2^5)
            22====(x1+x2+x3)/(x1-x2+x3)
            23====sin((x1+x2+x3)/(x1-x2+x3))^3.14159265357
            24====sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)
            25====sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)+sin(x1)+sin(x2)+sin(x3)-sin(x1+1)-sin(x1-1.1)-sin(x2-1)-sin(x2-1.1)+sin(x3+1)+sin(x3+2)+sin(x3+3*x1*x2*x3)
            26====cos(x1+x2-5*x3-x4-2*x5)+sin(2*x1+4*x2-5*x3-x4-2*x5)
            27====cos(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)+sin(2*x7+4*x8-5*x9^2-3*x10-2*x11)+sin(x9+x10-x7)+cos(x1+x2+x3)+12*x4
            28====sin(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)+sin(2)-cos(3)+tan(1.5)-sinh(4.22)+cos(4.15)
            29====(12*x1+3*x2-4*x3+5*x4-x5-4*x6+2*x7+x8-5*x9-x10-2*x11)
            30====(x1^2/sin(2*3.14159265357/x2))-x1/2
            31====(cos(1+exp(x1))/sqrt(sin(x1)^2-cos(x1)^2))+atan(x1)
            32====x1^3+x2^3+x3^3+x4^3
            33====x1^3.21+x2^3.14+x3^3+x4^3+x5^3+x6^3
            34====(sin(x1^3)-cos(x1^4)+tan(x1^0.5))/(2*x1^2+1)
            35====(sin(x1) + 2 + ((7-5) * (3.14159 * x1^(14-10)) + sin(-3.141) + (0%x1)) * x1/3 * 3/sqrt(x1+12))
            36====x1^3+x2^3+x3^3+x4^3+x5^3+x6^3
            37====sin(sqrt(x1^2+x2^2+x3^2))
            """;

    protected static final class BenchmarkExpressions {

        @FunctionalInterface
        public interface Eval {

            double apply(double[] x);
        }

        public static final Eval[] STATEMENTS = new Eval[]{
            /* 0 */x -> Math.pow((Math.sin(3) + Math.cos(4 - Math.sin(2))), (-2)),
            /* 1 */ x -> Math.sin(3) + Math.cos(5) - Math.pow(2.718281828459045, 2),
            /* 2 */ x -> Math.pow(((12 + 5) * 3 - Math.pow(2, 3) - 13 / 12.23), 3.2),
            /* 3 */ x -> 5 * Math.sin(3 + 2) / (4 * 3 - 2),
            /* 4 */ x -> (1 + 1) * (1 + 2) * (3 + 4) * (8 + 9) * (6 - 1) * (Math.pow(4, 3.14159265357)) - Math.pow((3 + 2), 1.8),
            /* 5 */ x -> (Math.sin(8 + Math.cos(3)) + 2 + ((27 - 5) / (Math.pow(8, 3)) * (3.14159 * Math.pow(4, (14 - 10))) + Math.sin(-3.141) + (0 % 4)) * 4 / 3 * 3 / Math.sqrt(4)) + 12,
            /* 6 */ x -> ((Math.pow(x[0], 2) + Math.sin(x[0])) / (1 + Math.cos(Math.pow(x[0], 2)))) * (Math.exp(x[0]) / 10),
            /* 7 */ x -> ((Math.pow(x[0], 2) + 3 * Math.sin(x[0] + Math.pow(5, 3) - 1 / 4)) / (23 / 33 + Math.cos(Math.pow(x[0], 2)))) * (Math.exp(x[0]) / 10),
            /* 8 */ x -> Math.exp(5 * 4 * 3 * 2 * 1),
            /* 9 */ x -> 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10 + 11 + 12 + 13 + 14 + 15 + 16 + 17 + 18 + 19 + 20,
            /* 10 */ x -> 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10 + 11 + 12 + 13 + 14 + 15 + 16 + 17 + 18 + 19 + 20 + Math.sin(x[0]),
            /* 11 */ x -> 2 + 3 * 4 - 5 / 2 + Math.sin(0) + Math.cos(0) + Math.sqrt(16),
            /* 12 */ x -> Math.sin(7 * x[0] + x[1]) + Math.cos(7 * x[0] - x[1]) - Math.sin(4) + Math.cos(Math.pow(5, 6)),
            /* 13 */ x -> ((Math.pow(x[0], 2) + 3 * Math.sin(x[0] + Math.pow(5, 3) - 1 / 4)) / (23 / 33 + Math.cos(Math.pow(x[0], 2)))) * (Math.exp(x[0]) / 10) + Math.pow((Math.sin(3) + Math.cos(4 - Math.sin(2))), (-2)),
            /* 14 */ x -> Math.pow((Math.pow(x[0], 2) + Math.pow(x[1], 0.5)), 4.2),
            /* 15 */ x -> Math.sin(Math.pow(x[0], 3) + Math.pow(x[1], 3)) - 4 * (x[0] - x[1]),
            /* 16 */ x -> Math.pow((x[0] + x[1] + x[2]), 0) + Math.pow((x[0] + x[1] + x[2]), 1) + Math.pow((x[0] + x[1] + x[2]), 2) + Math.pow((x[0] + x[1] + x[2]), 3) + Math.pow((x[0] + x[1] + x[2]), 4) + Math.pow((x[0] + x[1] + x[2]), 5) + Math.pow((x[0] + x[1] + x[2]), 6) + Math.pow((x[0] + x[1] + x[2]), 7),
            /* 17 */ x -> ((Math.pow(x[0], 2) + 3 * Math.sin(x[0] + Math.pow(5, 3) - 1 / 4 + 5 * x[1])) / (23 / 33 + Math.cos(Math.pow(x[0], 2)))) * (Math.exp(x[0] + 2 * Math.pow(x[2], 2)) / 10),
            /* 18 */ x -> Math.sin(x[0]) + 3 * Math.cos(x[0]) - 4 * Math.pow(x[0], 2) - 8 * Math.pow(x[0], 3) + 9 / (x[0] + 1) + 5 * Math.pow((x[0] - 1), 3) + 12 * x[1],
            /* 19 */ x -> Math.sin(Math.pow((x[0] + x[1] + x[2]), 3.14)),
            /* 20 */ x -> x[0] + x[1] + x[2],
            /* 21 */ x -> x[0] + x[1] + x[2] + Math.sin(2) - Math.cos(4) + Math.exp(Math.pow(2, 5)),
            /* 22 */ x -> (x[0] + x[1] + x[2]) / (x[0] - x[1] + x[2]),
            /* 23 */ x -> Math.pow(Math.sin((x[0] + x[1] + x[2]) / (x[0] - x[1] + x[2])), 3.14159265357),
            /* 24 */ x -> Math.sin(x[0]) + Math.sin(x[1]) + Math.sin(x[2]) - Math.sin(x[0] + 1) - Math.sin(x[0] - 1.1) - Math.sin(x[1] - 1) - Math.sin(x[1] - 1.1) + Math.sin(x[2] + 1) + Math.sin(x[2] + 2) + Math.sin(x[2] + 3 * x[0] * x[1] * x[2]),
            /* 25 */ x -> Math.sin(x[0]) + Math.sin(x[1]) + Math.sin(x[2]) - Math.sin(x[0] + 1) - Math.sin(x[0] - 1.1) - Math.sin(x[1] - 1) - Math.sin(x[1] - 1.1) + Math.sin(x[2] + 1) + Math.sin(x[2] + 2) + Math.sin(x[2] + 3 * x[0] * x[1] * x[2]) + Math.sin(x[0]) + Math.sin(x[1]) + Math.sin(x[2]) - Math.sin(x[0] + 1) - Math.sin(x[0] - 1.1) - Math.sin(x[1] - 1) - Math.sin(x[1] - 1.1) + Math.sin(x[2] + 1) + Math.sin(x[2] + 2) + Math.sin(x[2] + 3 * x[0] * x[1] * x[2]),
            /* 26 */ x -> Math.cos(x[0] + x[1] - 5 * x[2] - x[3] - 2 * x[4]) + Math.sin(2 * x[0] + 4 * x[1] - 5 * x[2] - x[3] - 2 * x[4]),
            /* 27 */ x -> Math.cos(12 * x[0] + 3 * x[1] - 4 * x[2] + 5 * x[3] - x[4] - 4 * x[5] + 2 * x[6] + x[7] - 5 * x[8] - x[9] - 2 * x[10]) + Math.sin(2 * x[6] + 4 * x[7] - 5 * Math.pow(x[8], 2) - 3 * x[9] - 2 * x[10]) + Math.sin(x[8] + x[9] - x[6]) + Math.cos(x[0] + x[1] + x[2]) + 12 * x[3],
            /* 28 */ x -> Math.sin(12 * x[0] + 3 * x[1] - 4 * x[2] + 5 * x[3] - x[4] - 4 * x[5] + 2 * x[6] + x[7] - 5 * x[8] - x[9] - 2 * x[10]) + Math.sin(2) - Math.cos(3) + Math.tan(1.5) - Math.sinh(4.22) + Math.cos(4.15),
            /* 29 */ x -> (12 * x[0] + 3 * x[1] - 4 * x[2] + 5 * x[3] - x[4] - 4 * x[5] + 2 * x[6] + x[7] - 5 * x[8] - x[9] - 2 * x[10]),
            /* 30 */ x -> (Math.pow(x[0], 2) / Math.sin(2 * 3.14159265357 / x[1])) - x[0] / 2,
            /* 31 */ x -> (Math.cos(1 + Math.exp(x[0])) / Math.sqrt(Math.pow(Math.sin(x[0]), 2) - Math.pow(Math.cos(x[0]), 2))) + Math.atan(x[0]),
            /* 32 */ x -> Math.pow(x[0], 3) + Math.pow(x[1], 3) + Math.pow(x[2], 3) + Math.pow(x[3], 3),
            /* 33 */ x -> Math.pow(x[0], 3.21) + Math.pow(x[1], 3.14) + Math.pow(x[2], 3) + Math.pow(x[3], 3) + Math.pow(x[4], 3) + Math.pow(x[5], 3),
            /* 34 */ x -> (Math.sin(Math.pow(x[0], 3)) - Math.cos(Math.pow(x[0], 4)) + Math.tan(Math.pow(x[0], 0.5))) / (2 * Math.pow(x[0], 2) + 1),
            /* 35 */ x -> (Math.sin(x[0]) + 2 + ((7 - 5) * (3.14159 * Math.pow(x[0], (14 - 10))) + Math.sin(-3.141) + (0 % x[0])) * x[0] / 3 * 3 / Math.sqrt(x[0] + 12)),
            /* 36 */ x -> Math.pow(x[0], 3) + Math.pow(x[1], 3) + Math.pow(x[2], 3) + Math.pow(x[3], 3) + Math.pow(x[4], 3) + Math.pow(x[5], 3),
            /* 37 */ x -> Math.sin(Math.sqrt(Math.pow(x[0], 2) + Math.pow(x[1], 2) + Math.pow(x[2], 2)))
        };
    }

    public static final StringBuilder EXPR_MAP = new StringBuilder();

    static {
        int i = 0;
        for (String e : EXPRESSIONS) {
            EXPR_MAP.append(i++).append("====").append(e).append("\n");
        }
    }

    /**
     * Run this to generate the expressions map
     * above({@linkplain ParserNGWars#EXPRESSIONS_MAP}) whenever the array is
     * updated. this will help users to know the index of the expression to
     * reference in the array from the benchmarks in
     * {@linkplain com.github.gbenroscience.parser.wars.individual.*}
     *
     * @param args
     */
    public static void main(String[] args) {
        
        System.out.println("Welcome to the Math Parser Benchmarks, aka ParserNG Wars.\n");
        System.out.println("HERE ARE THE AVAILABLE EXPRESSIONS, You may select one of them by the index:\n\n"+EXPRESSIONS_MAP);
        
        System.out.println("Choose an expression by typing the number before it\n");
        index = new Scanner(System.in).nextInt();
        if(index >= EXPRESSIONS.length){
            System.err.println("Invalid expression selected. Choose an index between 0 and "+EXPRESSIONS.length);
            return;
        }
        
        System.out.println("\n\nBenchmark Expression: "+EXPRESSIONS[index]+"\n\n");
        
        StringBuilder sb = new StringBuilder("Native Java - 0\n");
        sb.append("FieryJanino - 1\n");
        sb.append("BaseJanino - 2\n");
        sb.append("Paralithic - 3\n");
        sb.append("mXParser - 4\n");
        sb.append("exp4J - 5\n");
        sb.append("Parsii - 6\n");
        sb.append("ParserNG-Standard - 7\n");
        sb.append("ParserNG-Turbo(Array-Based) - 8\n");
        sb.append("ParserNG-Turbo(Widening-Args-Based) - 9\n");
        System.out.println(sb.toString());

        
        System.out.println("Welcome to the Math Parser Benchmarks, aka ParserNG Wars.\nEnter a comma separated list of the digits for the Math Parsers You want to Benchmark.");
        Scanner sc = new Scanner(System.in);

        String digitsCommand = sc.next();
        if (digitsCommand != null && !digitsCommand.trim().isEmpty()) {
            String[] digitsTextArray = digitsCommand.split(",");
            int[] digits = new int[digitsTextArray.length];
            try {
                for (int i = 0; i < digitsTextArray.length; i++) {
                    digits[i] = Integer.parseInt(digitsTextArray[i]);
                }
                OptionsBuilder opt = new OptionsBuilder();
                opt.include(Baseline.class.getSimpleName());
                StringBuilder versusBuilder = new StringBuilder();
                for (int i = 0; i < digitsTextArray.length; i++) {
                    switch (digits[i]) {
                        case 0:
                            opt.include(NativeJava.class.getSimpleName());
                            versusBuilder.append("NativeJava vs");
                            break;
                        case 1:
                            opt.include(FieryJanino.class.getSimpleName());
                            versusBuilder.append("FieryJanino vs");
                            break;
                        case 2:
                            opt.include(BaseJanino.class.getSimpleName());
                            versusBuilder.append("BaseJanino vs");
                            break;
                        case 3:
                            opt.include(Paralithic.class.getSimpleName());
                            versusBuilder.append("Paralithic vs");
                            break;
                        case 4:
                            opt.include(MxParser.class.getSimpleName());
                            versusBuilder.append("MxParser vs");
                            break;
                        case 5:
                            opt.include(Exp4J.class.getSimpleName());
                            versusBuilder.append("Exp4J vs");
                            break;
                        case 6:
                            opt.include(Parsii.class.getSimpleName());
                            versusBuilder.append("Parsii vs");
                            break;
                        case 7:
                            opt.include(ParserNGStandard.class.getSimpleName());
                            versusBuilder.append("ParserNG-Standard vs");
                            break;
                        case 8:
                            opt.include(ParserNGTurboArrayBased.class.getSimpleName());
                            versusBuilder.append("ParserNG-Turbo-Array-Based vs");
                            break;
                        case 9:
                            opt.include(ParserNGTurboWideningBased.class.getSimpleName());
                            versusBuilder.append("ParserNG-Turbo-Widening-Args-Based vs");
                            break;
                        default:
                            throw new AssertionError();
                    }
                }
                String st = versusBuilder.toString();
                String versus = st.substring(0, st.length() - 3);
                System.out.println(versus+"\n\nLET THE GAMES BEGIN!\n\n");
                
                opt = (OptionsBuilder) opt.mode(Mode.AverageTime)
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

            } catch (NumberFormatException e) {
                System.err.println("An error exists in your digits entry. Please try again.");
                e.printStackTrace();
                System.out.println(sb.toString());
            } catch (RunnerException ex) {
                System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }

    }

    public static void main2(String[] args) {//
        System.out.println(EXPR_MAP);
    }

}
