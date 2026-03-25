package com.github.gbenroscience.parser.wars.manual;

import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.FastCompositeExpression;
import com.github.gbenroscience.util.ConsoleTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * @author GBEMIRO
 */
public class ArrayFiller {

    private int mode;

    /**
     * To reduce memory pressure, let the libs fill one after the other. Discard
     * a bucket to claim back some memory after a fill
     *
     */
    static final int SERIAL = 0;
    /**
     * Both libs use separate thread to fill their buckets
     */
    static final int PARALLEL = 1;

    private int bucketSize;

    private CountDownLatch latch = new CountDownLatch(2);
    private CountDownLatch whistleLatch = new CountDownLatch(1);

    /**
     * A bit random data to compute the numbers
     */
    private int[] randomData;
    private volatile boolean gameOver;
    private final boolean showLogs;
    /**
     * Each run will be repeated this number of times or 1 if set to <= 0
     */
    private int repeatBenchmark = 1;

    public static final HashMap<String, double[]> perfs = new HashMap();

    /**
     *
     * @param mode
     * @param bucketSize
     * @param showLogs
     * @param repeatBenchmark The number of times to repeat the run on a
     * benchmark
     */
    public ArrayFiller(int mode, int bucketSize, boolean showLogs, int repeatBenchmark) {
        this.mode = mode;
        this.bucketSize = bucketSize;
        this.showLogs = showLogs;
        this.repeatBenchmark = repeatBenchmark;
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
    }

    private void println(String args) {
        if (showLogs) {
            System.out.println(args);
        }
    }

    private void reset() {
        println("reset called!!!!");
        this.randomData = Stats.splitLongIntoDigits(System.currentTimeMillis());
        gameOver = false;
        latch = new CountDownLatch(2);
        whistleLatch = new CountDownLatch(1);
    }

    static interface Lib {

        String getExpr();

        String getName();

        double solve(double x);

        /**
         *
         * @param count The number of times the benchmark has been repeated. Its
         * max value is {@link ArrayFiller#repeatBenchmark}
         */
        void compute(int count);

        void warmup();

        double getChecksum();

        long getErrorCount();

    }

    abstract class MathProducerLib implements Lib {

        private String name;
        private String expr;
        private double checksum;
        private long errorCount;
        private long duration;

        public MathProducerLib(String name, String expr) {
            this.name = name;
            this.expr = expr;
        }

        @Override
        public double getChecksum() {
            return checksum;
        }

        public long getErrorCount() {
            return errorCount;
        }

        @Override
        public String getExpr() {
            return expr;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void warmup() {
            double bucket[] = new double[10_000_000];
            int dataLen = randomData.length;
            long start = System.nanoTime();
            for (int i = 0; i < bucket.length; i++) {
                double x = randomData[i % dataLen];
                double res = bucket[i] = solve(x + (i / bucketSize));
                Double.isFinite(res);
            }
            duration = System.nanoTime() - start;
            println("Warmup for " + name + " ran in " + duration + "ns...bucket[0]=" + bucket[0]);
        }

        @Override
        public void compute(int count) {
            warmup();
            double bucket[] = new double[bucketSize];
            int dataLen = randomData.length;
            if (mode == SERIAL) {
                int i = 0;
                double localSum = 0; // Local variable for speed
                long localErrors = 0;
                long start = System.nanoTime();
                for (; i < bucketSize && !gameOver; i++) {
                    double x = randomData[i % dataLen];
                    double res = bucket[i] = solve(x + (i / bucketSize));
                    if (Double.isFinite(res)) {
                        localSum += res;
                    } else {
                        localErrors++; // Count NaN or Infinity
                    }
                }
                duration = System.nanoTime() - start;
                if (perfs.containsKey(name)) {
                    double[] arr = perfs.get(name);
                    arr[count - 1] = duration;
                } else {
                    double[] arr = new double[repeatBenchmark];
                    arr[count - 1] = duration;
                    perfs.put(name, arr);
                }
                this.checksum = localSum;
                this.errorCount = localErrors;
                println(getName() + " filled " + i + "/" + bucketSize + " in " + duration + " ns");
            } else {
                Thread.ofVirtual().start(() -> {
                    try {
                        println("Waiting for start-whistle signal");
                        whistleLatch.await();
                    } catch (InterruptedException ex) {
                        System.getLogger(ArrayFiller.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                    println(getName() + ">>> Ready to race...@" + System.currentTimeMillis());
                    int i = 0;
                    double localSum = 0; // Local variable for speed
                    long localErrors = 0;

                    long start = System.nanoTime();
                    for (; i < bucketSize; i++) {
                        double x = randomData[i % dataLen] + ((i * 1.0) / (1.0 * bucketSize));
                        double res = bucket[i] = solve(x);
                        if (Double.isFinite(res)) {
                            localSum += res;
                        } else {
                            localErrors++; // Count NaN or Infinity
                        }
                    }
                    duration = (System.nanoTime() - start);
                    if (perfs.containsKey(name)) {
                        double[] arr = perfs.get(name);
                        arr[count - 1] = duration;
                    } else {
                        double[] arr = new double[repeatBenchmark];
                        arr[count - 1] = duration;
                        perfs.put(name, arr);
                    }

                    this.checksum = localSum;
                    this.errorCount = localErrors;

                    i = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append(getName()).append("**********Duration: ").append(duration).append("ns********** ").append("---------------result-samples:\n");
                    for (; i < randomData.length; i++) {
                        double x = randomData[i % dataLen] + ((i * 1.0) / (1.0 * bucketSize));
                        sb.append("Lib ").append(getName()).append(": Sample data--> x = ").append(x).append(", ").append(solve(x)).append("\n");
                        i++;
                    }
                    println(sb.toString());

                    latch.countDown();
                });

            }
        }

    }

    final class ParserNGProducerLib extends MathProducerLib {

        private final MathExpression parserNG;
        private int xSlot;
        private int ySlot;
        private int zSlot;

        static {
            MathExpression.setAutoInitOn(true);
        }

        public ParserNGProducerLib(String expr) {
            super("ParserNG", expr);
            this.parserNG = new MathExpression(expr, true);
            this.xSlot = parserNG.getVariable("x").getFrameIndex();
            this.ySlot = parserNG.getVariable("y").getFrameIndex();
            this.zSlot = parserNG.getVariable("z").getFrameIndex();
        }

        @Override
        public double solve(double x) {
            parserNG.updateSlot(xSlot, x);
            parserNG.updateSlot(ySlot, x + 1);
            parserNG.updateSlot(zSlot, x + 2);
            return parserNG.solveGeneric().scalar;
        }

    }

    final class PNGTurboProducerLib extends MathProducerLib {

        private FastCompositeExpression fce;
        private int xSlot;
        private int ySlot;
        private int zSlot;

        double[] turboArgs = new double[4];

        static {
            MathExpression.setAutoInitOn(true);
        }

        public PNGTurboProducerLib(String expr) {
            super("ParserNG-Turbo", expr);
            MathExpression mex = new MathExpression(expr, true);
            try {
                this.fce = mex.compileTurbo();
                this.xSlot = mex.hasVariable("x") ? mex.getVariable("x").getFrameIndex() : -1;
                this.ySlot = mex.hasVariable("y") ? mex.getVariable("y").getFrameIndex() : -1;
                this.zSlot = mex.hasVariable("z") ? mex.getVariable("z").getFrameIndex() : -1;
            } catch (Throwable ex) {
                System.getLogger(ArrayFiller.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }

        @Override
        public double solve(double x) {
            if (xSlot >= 0) {
                turboArgs[xSlot] = x;
            }
            if (ySlot >= 0) {
                turboArgs[ySlot] = x + 1;
            }
            if (zSlot >= 0) {
                turboArgs[zSlot] = x + 2;
            }
            return fce.applyScalar(turboArgs);
        }

    }

    public interface JaninoMathFunction {

        double apply(double x, double y, double z);
    }

    final class Exp4JProducerLib extends MathProducerLib {

        private final Expression exp4J;

        public Exp4JProducerLib(String expr) {
            super("Exp4J", expr);
            this.exp4J = new ExpressionBuilder(expr).variables("x").variables("y").variables("z").build();
        }

        @Override
        public double solve(double x) {
            this.exp4J.setVariable("x", x);
            this.exp4J.setVariable("y", x+1);
            this.exp4J.setVariable("z", x+2);
            return this.exp4J.evaluate();
        }

    }

    final class JaninoProducerLib extends MathProducerLib {

        private final JaninoMathFunction fastEvaluator;

        public JaninoProducerLib(String expr) throws Exception {
            super("Janino", expr);

            // 1. Convert to Java syntax
            String javaExpr = MathToJaninoConverter.convert(expr);

            // 2. Use the "Pure" Interface
            // We use the STATIC method on ExpressionEvaluator to avoid manual setup
            this.fastEvaluator = (JaninoMathFunction) org.codehaus.janino.ExpressionEvaluator.createFastExpressionEvaluator(
                    javaExpr, // The expression
                    JaninoMathFunction.class, // Our single-method interface
                    new String[]{"x", "y", "z"}, // Parameter name
                    (ClassLoader) null // Use current thread's classloader
            );
        }

        @Override
        public double solve(double x) {
            // Direct method call, no reflection, no wrapper objects
            return fastEvaluator.apply(x, x + 1,x+2);
        }
    }

    private void analyze() {
        String lib = null;
        double time = Integer.MAX_VALUE * 100000.0;
        ArrayList<String> headers = new ArrayList<>();
        ArrayList<String> rowData = new ArrayList<>();
        headers.add("Bucket Size");
        rowData.add(bucketSize + "");

        for (Map.Entry<String, double[]> e : perfs.entrySet()) {
            String libName = e.getKey();
            double[] durations = e.getValue();

            String[][] data = new String[durations.length][2];
            for (int i = 0; i < durations.length; i++) {
                data[i][0] = String.valueOf(i + 1);
                data[i][1] = String.valueOf(durations[i]);
            }

            println("Lib " + libName + ":\n" + Arrays.toString(durations));
            ConsoleTable csTable = new ConsoleTable("Iterations(" + libName + ")-for bucket-size(" + bucketSize + ")", new String[]{"N", "Time"}, data);
            csTable.display();

            double duration = Stats.findAverage(durations);
            if (duration < time) {
                time = duration;
                lib = libName;
            }
            headers.add(libName);
            rowData.add(duration + "");
            println("Lib " + libName + " filled bucket(" + bucketSize + ") in " + duration + "ns");
        }
        rowData.add(lib);
        String[][] data = new String[][]{
            rowData.toArray(new String[0])
        };
        headers.add("Winner");
        println("Lib " + lib + " wins in " + time + "ns");

        ConsoleTable consoleTable = new ConsoleTable("BUCKET-FILLER-SHOOTOUTS", headers.toArray(new String[0]), data);
        consoleTable.display();

    }

    public void benchmark(String expr) {

        try {

            MathProducerLib[] libs = {new PNGTurboProducerLib(expr), new JaninoProducerLib(expr)/*, new Exp4JProducerLib(expr), new ParserNGProducerLib(expr)*/};
            for (MathProducerLib lib : libs) {
                int i = 1;
                while (i <= repeatBenchmark) {
                    lib.compute(i);
                    i++;
                }
            }
            if (mode == SERIAL) {
                analyze();
            }

            if (mode == PARALLEL) {
                java.util.Timer t = new java.util.Timer(true);
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        whistleLatch.countDown();
                    }
                }, 1500);

                try {
                    latch.await();
                    reset();
                } catch (InterruptedException intex) {
                    intex.printStackTrace();
                }

                double errorCount = -1;
                String lastLibName = null;
                println("\n--- FINAL VERIFICATION ---");
                if (showLogs) {
                    System.out.printf("%-15s | %-25s | %-15s%n", "Library", "Checksum", "Math Errors");
                }
                println("-----------------------------------------------------------------------");
                for (MathProducerLib lib : libs) {
                    System.out.printf("%-15s | %-25.4f | %-15d%n", lib.getName(), lib.getChecksum(), lib.getErrorCount());
                    if (errorCount == -1) {
                        errorCount = lib.getErrorCount();
                        lastLibName = lib.getName();
                    } else {
                        if (errorCount != lib.getErrorCount()) {
                            println("\nALERT: Disagreement on domain errors! Check for NaN handling differences... " + lastLibName + " VS " + lib.getName());
                            lastLibName = lib.getName();
                            errorCount = lib.getErrorCount();
                        }
                    }
                }
                analyze();
            }
        } catch (Exception ex) {
            System.getLogger(ArrayFiller.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

    public static void main(String[] args) {
        int i = 1;
        double N = 10.0 * Math.pow(2, 20);
        while (i < N) {
            ArrayFiller arf = new ArrayFiller(PARALLEL, (int) i, false, 10);
            System.err.println("BUCKET-SIZE =  " + i);
            //arf.benchmark("((x^2 + sin(x)) / (1 + cos(x^2))) * (exp(x) / 10)");
            arf.benchmark("(x^2/sin(2*3.14159265357/y))-x/2");
            //i = (int) Math.pow(2, i);
            i *= 10;
            // arf.benchmark("(x+y+z)^0+(x+y+z)^1+(x+y+z)^2+(x+y+z)^3+(x+y+z)^4+(x+y+z)^5+(x+y+z)^6+(x+y+z)^7");            
        }

    }

}
