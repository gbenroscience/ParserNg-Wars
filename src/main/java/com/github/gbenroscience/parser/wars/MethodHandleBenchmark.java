package com.github.gbenroscience.parser.wars;

/**
 *
 * @author GBEMIRO
 */
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.lang.invoke.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MethodHandleBenchmark {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // Test for 1, 8, 16, and 32 variables
    @Param({"1", "8", "16", "32"})
    public int varCount;

    private MethodHandle wideningHandle;
    private MethodHandle arrayHandle;
    private double[] argsArray;
    private Object[] wideningArgs;

    @Setup
    public void setup() throws Throwable {
        argsArray = new double[varCount];
        wideningArgs = new Object[varCount];
        for (int i = 0; i < varCount; i++) {
            argsArray[i] = i * 1.1;
            wideningArgs[i] = argsArray[i];
        }

        // 1. Array-based (ScalarTurboEvaluator1 style)
        MethodHandle sum = LOOKUP.findStatic(MethodHandleBenchmark.class, "sumArray",
                MethodType.methodType(double.class, double[].class, int.class));
        arrayHandle = MethodHandles.insertArguments(sum, 1, varCount);

        // 2. Widening (ScalarTurboEvaluator2 style)
        // First, find the method that takes the array
        MethodHandle baseHandle = LOOKUP.findStatic(MethodHandleBenchmark.class, "sumWide",
                MethodType.methodType(double.class, double[].class));

        // Then, transform it to accept 'varCount' individual double arguments
        // This creates a handle with the signature (double, double, ... N times) -> double
        wideningHandle = baseHandle.asCollector(double[].class, varCount);
    }

// Update this method to take a standard array (no varargs) to avoid confusion
    public static double sumWide(double[] vars) {
        double s = 0;
        for (double v : vars) {
            s += v;
        }
        return s;
    }

    public static double sumArray(double[] vars, int count) {
        double s = 0;
        for (int i = 0; i < count; i++) {
            s += vars[i];
        }
        return s;
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testArrayPassing(Blackhole bh) throws Throwable {
        bh.consume((double) arrayHandle.invokeExact(argsArray));
    }

    @org.openjdk.jmh.annotations.Benchmark
    public void testWideningPassing(Blackhole bh) throws Throwable {
        // We use invokeWithArguments to simulate the dynamic nature of the widening handle
        bh.consume((double) wideningHandle.invokeWithArguments(wideningArgs));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MethodHandleBenchmark.class.getSimpleName())
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
}
