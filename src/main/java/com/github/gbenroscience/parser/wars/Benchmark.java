package com.github.gbenroscience.parser.wars;

import com.expression.parser.Parser;
import com.github.gbenroscience.logic.DRG_MODE;
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.util.ConsoleTable;
import com.github.gbenroscience.util.FunctionManager;
import com.github.gbenroscience.util.VariableManager; 
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author GBEMIRO
 */
public class Benchmark {

    public static final String PARSER_NG = "ParserNG";
    public static final String JAVA_MEP = "JavaMEP";
    public static final String EXP_4J = "Exp4J";

    //The math expression string
    private String expression;
    /**
     * Store objects that need pre-compilation to work here
     */
    private final HashMap<String, Object> objectPool = new HashMap<>();
    private final HashMap<String, Method> libsRegistry = new HashMap<>();

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

    public static interface Method {

        double run();
    }

    public static double findAverage(double[] data) {
        if (data == null || data.length == 0) {
            return Double.NaN;
        }
        if (data.length <= 3) {
            // Too few points for meaningful outlier removal – return simple average
            double sum = 0.0;
            for (double v : data) {
                sum += v;
            }
            return sum / data.length;
        }

        // Work on a copy to avoid modifying the original array
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        int n = sorted.length;

        // More accurate quartile calculation with linear interpolation (Type 7 / "default" method)
        double q1 = percentile(sorted, 25.0);
        double q3 = percentile(sorted, 75.0);
        double iqr = q3 - q1;

        // Use standard 1.5 * IQR fences (Tukey method)
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        // Count inliers and compute sum for average
        double sum = 0.0;
        int count = 0;
        for (double v : sorted) {
            if (v >= lowerBound && v <= upperBound) {
                sum += v;
                count++;
            }
        }

        // Fallback if all values were outliers (rare but possible with extreme noise)
        if (count == 0) {
            // Return overall average as safe fallback
            sum = 0.0;
            for (double v : data) {
                sum += v;
            }
            return sum / data.length;
        }

        return sum / count;
    }

// Helper: percentile with linear interpolation (matches R's default, Python's numpy.percentile, etc.)
    private static double percentile(double[] sorted, double p) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        if (p <= 0) {
            return sorted[0];
        }
        if (p >= 100) {
            return sorted[sorted.length - 1];
        }

        double index = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        double fraction = index - lower;

        if (lower + 1 < sorted.length) {
            return sorted[lower] + fraction * (sorted[lower + 1] - sorted[lower]);
        } else {
            return sorted[lower];
        }
    }

    private double round(double val, int n) {
        double scale = Math.pow(10, n);
        return Math.round(val * scale) / scale;
    }

    private void registerLib(String libName, String expression) {
        this.expression = expression;
        if (!expression.equals(this.expression)) {
            this.expression = expression;
            objectPool.clear();
            libsRegistry.clear();
        }
        Object o = objectPool.get(libName);
        if (o == null) {
            switch (libName) {
                case PARSER_NG:
                    MathExpression me = new MathExpression(expression);
                    me.setDRG(DRG_MODE.RAD);
                    objectPool.put(libName, me);
                    break;
                case JAVA_MEP:

                    break;
                case EXP_4J:
                    Expression calc = new ExpressionBuilder(expression)
                            .build();
                    objectPool.put(libName, calc);
                    break;

                default:
                    break;
            }
        }

        defineRunMethod(libName);

    }

    private void defineRunMethod(String libName) {
        libsRegistry.put(libName, () -> {
            switch (libName) {
                case PARSER_NG:
                    MathExpression m = (MathExpression) objectPool.get(libName);
                    return m.solveGeneric().scalar;
                case JAVA_MEP:
                    return Parser.simpleEval(expression);
                case EXP_4J:
                    Expression calc = (Expression) objectPool.get(libName);
                    
                    return calc.evaluate();
                default:
                    return Double.NaN;
            }
        });
    }

   

    public Result runLib(String libName, int n) {
        Method mthd = libsRegistry.get(libName);
        if (mthd == null) {
            return new Result(Double.NaN, 0, 0, 0);
        }

        // JIT Warm-up for high-count runs
        int warmUp = (n > 100) ? 20000 : 10;
        for (int i = 0; i < warmUp; i++) {
            mthd.run();
        }
        try {
            Thread.sleep(Duration.ofMillis(10));
        } catch (InterruptedException ex) {
            System.getLogger(Benchmark.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        double[] durations = new double[n];
        double min = Double.MAX_VALUE;
        double max = 0;
        double lastValue = 0;

        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            lastValue = mthd.run();
            long duration = System.nanoTime() - start;

            durations[i] = (double) duration;
            if (duration < min) {
                min = duration;
            }
            if (duration > max) {
                max = duration;
            }
        }

        // Applying your IQR-based findAverage to clean the data
        double avg = findAverage(durations);

        return new Result(round(avg, 2), min, max, lastValue);
    }

    public void runBenchmarks(String expr, int[] sampleSize, String... libs) {

        for (String lib : libs) {
            registerLib(lib, expr);
        }

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
                    String lib = libs[j - 1];
                    Result r = runLib(lib, n);
                    columnsWithAvg[j] = String.valueOf(r.avg);
                    columnsWithMin[j] = String.valueOf(r.min);
                    columnsWithMax[j] = String.valueOf(r.max);
                    int index = j + libs.length;
                    columnsWithAvg[index] = String.valueOf(r.value);//1->4, 2->5, 3->6// n|Pa|Jv|Ex|V(Pa)|V(Jv)|V(Ex)|
                    columnsWithMin[index] = String.valueOf(r.value);
                    columnsWithMax[index] = String.valueOf(r.value);

                    // Winner comparisons
                    if (winningLibNameMin == null) {
                        winningLibNameMin = lib;
                        winningLibNameMax = lib;
                        winningLibNameAvg = lib;
                        winningMin = r.min;
                        winningMax = r.max;
                        winningAvg = r.avg;
                    } else {
                        if (winningMin > r.min) {
                            winningLibNameMin = lib;
                            winningMin = r.min;
                            tieMin = false;
                            tieLibsMin = "";
                        } else if (winningMin == r.min) {
                            tieMin = true;
                            tieLibsMin = tieLibsMin.isEmpty() ? winningLibNameMin + "-" + lib : tieLibsMin + "-" + winningLibNameMin;
                        }
                        if (winningMax > r.max) {
                            winningLibNameMax = lib;
                            winningMax = r.max;
                            tieMax = false;
                            tieLibsMax = "";
                        } else if (winningMax == r.max) {
                            tieMax = true;
                            tieLibsMax = tieLibsMax.isEmpty() ? winningLibNameMax + "-" + lib : tieLibsMax + "-" + winningLibNameMax;
                        }
                        if (winningAvg > r.avg) {
                            winningLibNameAvg = lib;
                            winningAvg = r.avg;
                            tieAvg = false;
                            tieLibsAvg = "";
                        } else if (winningAvg == r.avg) {
                            tieAvg = true;
                            tieLibsAvg = tieLibsAvg.isEmpty() ? winningLibNameAvg + "-" + lib : tieLibsAvg + "-" + winningLibNameAvg;
                        }
                    }
                    System.out.println("Benchmark done for Lib[" + lib + "] for iterations[" + n + "]");
                } else {
                    if (j == columnsWithAvg.length - 1) {
                        columnsWithAvg[j] = tieAvg ? ensureShortNamesForTyingLibs(tieLibsAvg, 3) : winningLibNameAvg;
                        columnsWithMin[j] = tieMin ? ensureShortNamesForTyingLibs(tieLibsMin, 3) : winningLibNameMin;
                        columnsWithMax[j] = tieMax ? ensureShortNamesForTyingLibs(tieLibsMax, 3) : winningLibNameMax;
                    }
                }
            }

        }

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

    /**
     * Shrotens the name of the tying libs e.g from ParserNG-Exp4J-JavaMEP to
     * Par-Exp-Jav
     *
     * @param tyingLibs The name of the tying libs e.g ParserNG-Exp4J-JavaMEP
     * @param charsPerLibName How many characters to use from the start of each
     * lib name
     * @return a shortened but relatable form of the tying libs string
     */
    private String ensureShortNamesForTyingLibs(String tyingLibs, int charsPerLibName) {
        String[] split = tyingLibs.split("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            sb.append(s.length() <= charsPerLibName ? s : s.substring(0, charsPerLibName)).append("-");
        }
        return sb.substring(0, sb.length() - 1);
    }

    public static void main(String args[]) {
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
        String s17 = "(7+2)*(3+5)*(9-2)";//ParserNG is really good at this
        

        new Benchmark().runBenchmarks(s17, new int[]{1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000, 5000000, 10000000},
                 PARSER_NG, EXP_4J, JAVA_MEP);

    }

}
