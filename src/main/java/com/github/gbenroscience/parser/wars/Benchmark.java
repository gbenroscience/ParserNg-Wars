package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.FastExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import com.github.gbenroscience.util.ConsoleTable;
import com.github.gbenroscience.util.FunctionManager;
import com.github.gbenroscience.util.VariableManager;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author GBEMIRO
 */
public class Benchmark {

    public static final String PARSER_NG_TURBO = "PngTurbo";
    public static final String PARSER_NG = "ParserNG";
    public static final String JAVA_MEP = "JavaMEP";
    public static final String EXP_4J = "Exp4J";
    public static final String JANINO = "Janino";
    private static final int MAX_ARGS = 20;

    public static interface JaninoMathFunction {

        double apply(double x, double y, double z);
    }
    private String expression;
    int dataLen = 0;

    /**
     * A cache of double arrays that registered libs will be reusing to return
     * their x,y,z last values
     */
    private static double[][] cache = new double[20][2];

    /**
     * A bit random data to compute the numbers
     */
    private int[] randomData;

    private int warmups = 20_000;

    public Benchmark() {
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        dataLen = randomData.length;
    }

    public void setup() {
        MathExpression.setAutoInitOn(true);
        // ParserNG - compile once

        /*
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
     
         */
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        dataLen = randomData.length;

    }

    public final class Result {

        private final double avg;
        private final double min;
        private final double max;
        private final double value;

        public Result(double avg, double min, double max, double value) {
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format(
                    "{\n  \"avg\": %.2f,\n  \"min\": %.2f,\n  \"max\": %.2f,\n  \"value\": %s\n}",
                    avg, min, max, value
            );
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getValue() {
            return value;
        }

        public double getAvg() {
            return avg;
        }

    }

    static interface Lib {

        int getCacheIdx();

        String getName();

        double solve();

        Result compute(int n);
    }

    abstract class MathLib implements Lib {

        private String name;
        private long duration;
        private final int cacheIdx;
        double[] lastArgValues = new double[MAX_ARGS];

        protected AtomicInteger cursor = new AtomicInteger();

        public MathLib(String name, int cacheIdx) {
            this.cacheIdx = cacheIdx;
            this.name = name;
            setup();
        }

        protected abstract void setup();

        @Override
        public String getName() {
            return name;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getCacheIdx() {
            return cacheIdx;
        }

        public Result compute(int n) {
            double v[] = new double[1];
            System.out.println("WARMING UP LIB(" + name + ") with " + warmups + " runs");
            for (int i = 0; i < warmups; i++) {
                v[0] = this.solve();
            }

            System.out.println("WARM-UPS FOR  LIB(" + name + ") with " + warmups + " COMPLETED");
            v[0] /= 2;
            double[] durations = new double[n];
            double min = Double.MAX_VALUE;
            double max = 0;
            double lastValue = 0;

            for (int i = 0; i < n; i++) {
                long start = System.nanoTime();
                lastValue = this.solve();
                long d = System.nanoTime() - start;

                durations[i] = (double) d;
                if (d < min) {
                    min = d;
                }
                if (d > max) {
                    max = d;
                }
            }

            double avg = Stats.findAverage(durations);
            return new Result(Stats.round(avg, 2), min, max, lastValue);
        }

    }

    class Janino extends MathLib {

        private JaninoMathFunction fastEvaluator;

        public Janino(int cacheIdx) {
            super(JANINO, cacheIdx);
        }

        @Override
        protected void setup() {
            try {
                org.codehaus.janino.ClassBodyEvaluator cbe = new org.codehaus.janino.ClassBodyEvaluator();

                // 1. Tell Janino which interface we are implementing
                cbe.setImplementedInterfaces(new Class[]{JaninoMathFunction.class});

                // 2. Build the actual Java class body as a string
                // This removes all ambiguity about method names or parameters
                String classBody
                        = "public double apply(double x, double y, double z) {\n"
                        + "    return " + MathToJaninoConverter.convert(expression) + ";\n"
                        + "}";

                cbe.cook(classBody);

                // 3. Instantiate the generated class
                this.fastEvaluator = (JaninoMathFunction) cbe.getClazz().getDeclaredConstructor().newInstance();

            } catch (Exception ex) {
                ex.printStackTrace();
                System.getLogger(ParserNGWars.class.getName()).log(System.Logger.Level.ERROR, "Janino ClassBody Setup Failed", ex);
            }
        }

        @Override
        public double solve() {
            double x = randomData[cursor.getAndIncrement() % dataLen];
            double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
            double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;

            this.lastArgValues[0] = x;
            this.lastArgValues[1] = y;
            this.lastArgValues[2] = z;

            return fastEvaluator.apply(x, y, z);
        }

    }

    class Exp4J extends MathLib {

        private Expression calc;

        public Exp4J(int cacheIdx) {
            super(EXP_4J, cacheIdx);
        }

        @Override
        protected void setup() {
            this.calc = new ExpressionBuilder(expression).variable("x").variable("y").variable("z")
                    .build();
        }

        @Override
        public double solve() {

            double x = randomData[cursor.getAndIncrement() % dataLen];
            double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
            double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;

            calc.setVariable("x", x);
            calc.setVariable("y", y);
            calc.setVariable("z", z);

            this.lastArgValues[0] = x;
            this.lastArgValues[1] = y;
            this.lastArgValues[2] = z;
            return calc.evaluate();
        }

    }

    class ParserNG extends MathLib {

        private MathExpression me;
        int xSlot;
        int ySlot;
        int zSlot;

        public ParserNG(int cacheIdx) {
            super(PARSER_NG, cacheIdx);
        }

        @Override
        protected void setup() {
            me = new MathExpression(expression, true);

            this.xSlot = this.me.hasVariable("x") ? this.me.getVariable("x").getFrameIndex() : -1;
            this.ySlot = this.me.hasVariable("y") ? this.me.getVariable("y").getFrameIndex() : -1;
            this.zSlot = this.me.hasVariable("z") ? this.me.getVariable("z").getFrameIndex() : -1;
        }

        @Override
        public double solve() {

            double x = randomData[cursor.getAndIncrement() % dataLen];
            double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
            double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;

            me.updateSlot(xSlot, x);
            me.updateSlot(ySlot, y);
            me.updateSlot(zSlot, z);

            this.lastArgValues[0] = x;
            this.lastArgValues[1] = y;
            this.lastArgValues[2] = z;
            return me.solveGeneric().scalar;
        }

    }

    class ParserNGTurbo extends MathLib {

        private FastCompositeExpression fce;
        double[] turboArgs = new double[11];
        int xSlot;
        int ySlot;
        int zSlot;

        public ParserNGTurbo(int cacheIdx) {
            super(PARSER_NG_TURBO, cacheIdx);
        }

        @Override
        protected void setup() {
            try {
                MathExpression me = new MathExpression(expression, true);
                this.fce = new ScalarTurboEvaluator(me, false).compile();

                this.xSlot = me.hasVariable("x") ? me.getVariable("x").getFrameIndex() : -1;
                this.ySlot = me.hasVariable("y") ? me.getVariable("y").getFrameIndex() : -1;
                this.zSlot = me.hasVariable("z") ? me.getVariable("z").getFrameIndex() : -1;

            } catch (Throwable ex) {
                System.getLogger(Benchmark.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }

        }

        @Override
        public double solve() {

            double x = randomData[cursor.getAndIncrement() % dataLen];
            double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
            double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;
            if (xSlot >= 0) {
                turboArgs[xSlot] = x;
                this.lastArgValues[0] = x;
            }
            if (ySlot >= 0) {
                turboArgs[ySlot] = y;
                this.lastArgValues[1] = y;
            }
            if (zSlot >= 0) {
                turboArgs[zSlot] = z;
                this.lastArgValues[2] = z;
            }
            return fce.applyScalar(turboArgs);
        }
    }

    class JavaMEP extends MathLib {

        private FastCompositeExpression fce;
        double[] turboArgs = new double[11];

        public JavaMEP(int cacheIdx) {
            super(JAVA_MEP, cacheIdx);
        }

        @Override
        protected void setup() {

        }

        @Override
        public double solve() {
            double x = randomData[cursor.getAndIncrement() % dataLen];
            double y = randomData[cursor.getAndIncrement() % dataLen] - 0.5;
            double z = randomData[cursor.getAndIncrement() % dataLen] - 0.25;

            this.lastArgValues[0] = x;
            this.lastArgValues[1] = y;
            this.lastArgValues[2] = z;

            return com.expression.parser.Parser.eval(expression, new String[]{"x", "y", "z"}, new Double[]{x, y, z});
        }
    }

    private String ensureShortNamesForTyingLibs(String tyingLibs, int charsPerLibName) {
        String[] split = tyingLibs.split("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            sb.append(s.length() <= charsPerLibName ? s : s.substring(0, charsPerLibName)).append("-");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public void runBenchmarks(String expr, int[] sampleSize, String... libs) {
        this.expression = expr;
        MathLib mathLibs[] = new MathLib[libs.length];
        for (int i = 0; i < libs.length; i++) {
            switch (i) {
                case 0:
                    mathLibs[0] = new Janino(i);
                    break;
                case 1:
                    mathLibs[1] = new ParserNGTurbo(i);
                    break;
                case 2:
                    mathLibs[2] = new ParserNG(i);
                    break;
                case 3:
                    mathLibs[3] = new Exp4J(i);
                    break;
                default:
                    break;
            }
        }

        //Register done!
        String[][] dataWithAvg = new String[sampleSize.length][2 * libs.length + 2];
        String[][] dataWithMin = new String[sampleSize.length][2 * libs.length + 2];
        String[][] dataWithMax = new String[sampleSize.length][2 * libs.length + 2];

        for (int i = 0; i < sampleSize.length; i++) {
            int n = sampleSize[i];

            String[] columnsWithAvg = dataWithAvg[i];
            String columnsWithMin[] = dataWithMin[i];
            String columnsWithMax[] = dataWithMax[i];

            String winningLibNameMin = null, winningLibNameMax = null, winningLibNameAvg = null;
            double winningMin = Double.NaN, winningMax = Double.NaN, winningAvg = Double.NaN;
            String tieLibsMin = "", tieLibsMax = "", tieLibsAvg = "";
            boolean tieAvg = false, tieMin = false, tieMax = false;

            for (int j = 0; j < columnsWithAvg.length; j++) {
                if (j == 0) {
                    columnsWithAvg[j] = columnsWithMin[j] = columnsWithMax[j] = String.valueOf(n);
                } else if (j >= 1 && j <= libs.length) {
                    MathLib lib = mathLibs[j - 1];
                    Result r = lib.compute(n);
                    columnsWithAvg[j] = String.valueOf(r.avg);
                    columnsWithMin[j] = String.valueOf(r.min);
                    columnsWithMax[j] = String.valueOf(r.max);
                    int index = j + libs.length;
                    columnsWithAvg[index] = String.valueOf(r.value);//1->4, 2->5, 3->6// n|Pa|Jv|Ex|V(Pa)|V(Jv)|V(Ex)|
                    columnsWithMin[index] = String.valueOf(r.value);
                    columnsWithMax[index] = String.valueOf(r.value);

                    // Winner comparisons
                    if (winningLibNameMin == null) {
                        winningLibNameMin = lib.name;
                        winningLibNameMax = lib.name;
                        winningLibNameAvg = lib.name;
                        winningMin = r.min;
                        winningMax = r.max;
                        winningAvg = r.avg;
                    } else {
                        if (winningMin > r.min) {
                            winningLibNameMin = lib.name;
                            winningMin = r.min;
                            tieMin = false;
                            tieLibsMin = "";
                        } else if (winningMin == r.min) {
                            tieMin = true;
                            tieLibsMin = tieLibsMin.isEmpty() ? winningLibNameMin + "-" + lib.name : tieLibsMin + "-" + winningLibNameMin;
                        }
                        if (winningMax > r.max) {
                            winningLibNameMax = lib.name;
                            winningMax = r.max;
                            tieMax = false;
                            tieLibsMax = "";
                        } else if (winningMax == r.max) {
                            tieMax = true;
                            tieLibsMax = tieLibsMax.isEmpty() ? winningLibNameMax + "-" + lib.name : tieLibsMax + "-" + winningLibNameMax;
                        }
                        if (winningAvg > r.avg) {
                            winningLibNameAvg = lib.name;
                            winningAvg = r.avg;
                            tieAvg = false;
                            tieLibsAvg = "";
                        } else if (winningAvg == r.avg) {
                            tieAvg = true;
                            tieLibsAvg = tieLibsAvg.isEmpty() ? winningLibNameAvg + "-" + lib.name : tieLibsAvg + "-" + winningLibNameAvg;
                        }
                    }
                    System.out.println("Benchmark done for Lib[" + lib.name + "] for iterations[" + n + "]");
                } else {
                    if (j == columnsWithAvg.length - 1) {
                        columnsWithAvg[j] = tieAvg ? ensureShortNamesForTyingLibs(tieLibsAvg, 3) : winningLibNameAvg;
                        columnsWithMin[j] = tieMin ? ensureShortNamesForTyingLibs(tieLibsMin, 3) : winningLibNameMin;
                        columnsWithMax[j] = tieMax ? ensureShortNamesForTyingLibs(tieLibsMax, 3) : winningLibNameMax;
                    }
                }
            }
        }//end outer for loop

        String headers[] = new String[2 * libs.length + 2];
        String headersMin[] = new String[2 * libs.length + 2];
        String headersMax[] = new String[2 * libs.length + 2];

        for (int i = 0; i < headers.length - 1; i++) {
            if (i == 0) {
                headers[i] = headersMin[i] = headersMax[i] = "n";
            } else if (i >= 1 && i <= libs.length) {
                headers[i] = libs[i - 1] + "(ns)avg";
                headersMin[i] = libs[i - 1] + "(ns)min";
                headersMax[i] = libs[i - 1] + "(ns)max";
            } else {
                int index = i - (libs.length + 1);
                headers[i] = headersMin[i] = headersMax[i] = "Values(" + libs[index] + ")";
            }
        }
        headers[headers.length - 1] = headersMin[headers.length - 1] = headersMax[headers.length - 1] = "Winner";

        new ConsoleTable("RESULTS for [" + expr + "] Average time(nanos). ", headers, dataWithAvg).display();
        new ConsoleTable("RESULTS for [" + expr + "] Fastest time(nanos)", headersMin, dataWithMin).display();
        new ConsoleTable("RESULTS for [" + expr + "] Slowest time(nanos)", headersMax, dataWithMax).display();

    }

    public static void main(String[] args) {
        System.out.println("5! + 9Р3 + 6Č5-->>" + new MathExpression("5! + 9Р3 + 6Č5").solve());
        MathExpression mex = new MathExpression("A=@(3,3)(3,4,2,9,12,5,4,1,2);eigpoly(A);");
        System.out.println(FunctionManager.FUNCTIONS);
        System.out.println("----------" + mex.solve());

        MathExpression.setAutoInitOn(true);
        String s = "sqrt(0.64-x^2)";
        System.out.println("--->" + new MathExpression(s).solve());
        System.out.println("VARIABLES: " + VariableManager.VARIABLES);

        String s7 = "5*sin(3+2)/(4*3-2)";
        String s6 = "5*(3+2)/(4*3-2)";
        String s8 = "((12+5)*3 - (45/9))^2";
        String s9 = "5! + 9Р3 + 6Č5";
        String s10 = "√81 + ³√27 + 2^10";
        String s11 = "(sin(3)+cos(4-sin(2)))^(-2)";
        String s12 = "(2*sin(5)+3*cos(5))^2";
        String s13 = "cos(sin(2))";//Exp4J is really good at this
        String s14 = "cos(sin(2+tan(7)))";//Exp4J is really good at this
        String s15 = "cos(sin(2+tan(7^3-5^3)))";//Exp4J is really good at this
        String s16 = "sin(3)+cos(5)-2.718281828459045^2";//ParserNG is cool here
        String s17 = "(7+2)*(3+5)*(9-2)+sin(12^5)/cos(18^0.5)-(4+8)^0.5";//ParserNG is really good at this
        String s18 = "(sin(x^3)-cos(x^4)+tan(x^0.5))/(2*x^2+1)";
        String s19 = "(x^2/sin(2*3.14159265357/y))-x/2";

        int[] slopySamples = new int[]{1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000, 5000000, 10000000};
        int[] cpuCruncherSamples = new int[]{25_000_000, 30_000_000, 35_000_000, 40_000_000, 45_000_000, 50_000_000};

        new Benchmark().runBenchmarks(s18, cpuCruncherSamples,
                /*PARSER_NG, EXP_4J, */ JANINO, PARSER_NG_TURBO/*, JAVA_MEP*/);
    }

}
