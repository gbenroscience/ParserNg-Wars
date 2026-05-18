package com.github.gbenroscience.parser.wars.individual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.wars.Stats;
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
    protected FastCompositeExpression arrayBasedTurbo;
    protected FastCompositeExpression wideningBasedTurbo; 

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

    static int index = EXPRESSIONS.length - 1;
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
        System.out.println("EXPRESSION: "+EXPRESSION);
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
    
    protected void initRandomData(){
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
    public static void main1(String[] args) {//
        System.out.println(EXPR_MAP);
    }

}
