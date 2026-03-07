package com.github.gbenroscience.parser.wars;

import com.github.gbenroscience.parser.MathExpression;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private volatile boolean started;

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

    /**
     * A bit random data to compute the numbers
     */
    private int[] randomData;
    private volatile boolean gameOver;

    public ArrayFiller(int mode, int bucketSize) {
        this.mode = mode;
        this.bucketSize = bucketSize;
        this.randomData = splitLongIntoDigits(System.currentTimeMillis());
    }

    public final static int[] splitLongIntoDigits(long n) {
        if (n == 0) {
            return new int[]{0}; // Special case for zero
        }

        boolean isNegative = n < 0;
        long temp = Math.abs(n); // Work with the absolute value
        List<Integer> digitList = new ArrayList<>();

        while (temp > 0) {
            // Get the last digit using modulo 10
            digitList.add((int) (temp % 10));
            // Remove the last digit using integer division
            temp /= 10;
        }

        // The digits are in reverse order, so reverse the list
        Collections.reverse(digitList);

        // Convert the ArrayList<Integer> to a primitive int[] array
        int[] digits = new int[digitList.size()];
        for (int i = 0; i < digitList.size(); i++) {
            digits[i] = digitList.get(i);
        }

        // Handle the sign if necessary (e.g. if the original number was negative, you might need 
        // to handle the representation of the sign explicitly, depending on requirements)
        // For simply getting the sequence of digits, the absolute value is sufficient.
        return digits;
    }

    private void reset() {
        System.out.println("reset called!!!!");
        this.randomData = splitLongIntoDigits(System.currentTimeMillis());
        started = false;
        gameOver = false;
        latch = new CountDownLatch(2);
    }

    static interface Lib {

        String getExpr();

        String getName();

        double solve(double x);

        void compute();
    }

    abstract class MathProducerLib implements Lib {

        private String name;
        private String expr;

        public MathProducerLib(String name, String expr) {
            this.name = name;
            this.expr = expr;
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
        public void compute() {
            double bucket[] = new double[bucketSize];
            int dataLen = randomData.length;
            if (mode == SERIAL) {
                int i = 0;
                long start = System.nanoTime();
                for (; i < bucketSize && !gameOver; i++) {
                    double x = randomData[i % dataLen];
                    bucket[i] = solve(x + (i / bucketSize));
                }
                double end = System.nanoTime() - start;
                System.out.println(getName() + " filled " + i + "/" + bucketSize + " in " + end + " ns");
            } else {
                Thread.ofVirtual().start(() -> {
                    while (!started) {
                        try {
                            System.out.println(getName() + " Waiting for start signal");
                            Thread.sleep(Duration.ofMillis(100));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    System.out.println(getName() + " Ready to race...");
                    int i = 0;
                    double start = System.nanoTime();
                    for (; i < bucketSize && !gameOver; i++) {
                        double x = randomData[i % dataLen] + ((i * 1.0) / (1.0 * bucketSize));
                        bucket[i] = solve(x);
                    }
                    double end = System.nanoTime() - start;
                    if (i == bucketSize) {
                        gameOver = true;
                        System.out.println("Lib " + getName() + " wins!\nFilled bucket of size = " + bucketSize + " fully in " + end + "ns");
                    } else {
                        System.out.println("Lib " + getName() + " lost!\nPartially filled bucket of size = " + bucketSize + " with " + i + " items in " + end + "ns");
                    }
                    i = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append(getName()).append("----------------------------------result-samples:");
                    for (; i < randomData.length; i++) {
                        double x = randomData[i % dataLen] + ((i * 1.0) / (1.0 * bucketSize));
                        sb.append("Lib ").append(getName()).append(", x = ").append(x).append(", ").append(solve(x)).append(", bucket[i] = ").append(bucket[i]).append("\n");
                        i++;
                    }
                    System.out.println(sb.toString());

                    latch.countDown();
                });

            }
        }

    }

    final class ParserNGProducerLib extends MathProducerLib {

        private final MathExpression parserNG;

        static {
            MathExpression.setAutoInitOn(true);
        }

        public ParserNGProducerLib(String expr) {
            super("ParserNG", expr);
            this.parserNG = new MathExpression(expr);
        }

        @Override
        public double solve(double x) {
            parserNG.setValue("x", x);
            return parserNG.solveGeneric().scalar;
        }

    }

    final class Exp4JProducerLib extends MathProducerLib {

        private final Expression exp4J;

        public Exp4JProducerLib(String expr) {
            super("Exp4J", expr);
            this.exp4J = new ExpressionBuilder(expr).variables("x").build();
        }

        @Override
        public double solve(double x) {
            this.exp4J.setVariable("x", x);
            return this.exp4J.evaluate();
        }

    }

    public void benchmark(String expr) {

        ParserNGProducerLib pngpl = new ParserNGProducerLib(expr);
        Exp4JProducerLib ejpl = new Exp4JProducerLib(expr);

        ejpl.compute();
        pngpl.compute();
        if (mode == PARALLEL) {
            java.util.Timer t = new java.util.Timer(true);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    started = true;
                }
            }, 2000);
            try {
                latch.await();
                reset();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ArrayFiller arf = new ArrayFiller(SERIAL, 10000000);
        /*
"sin(x)+3*cos(x)"
         */
        arf.benchmark("((x^2 + sin(x)) / (1 + cos(x^2))) * (exp(x) / 10)");
    }

}
