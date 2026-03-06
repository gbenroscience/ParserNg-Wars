/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.github.gbenroscience.parser.wars;
 

import com.expression.parser.Parser;
import com.github.gbenroscience.parser.MathExpression;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * JMH Benchmark comparing ParserNG, Exp4J, and JavaMEP.
 * Focus: repeated evaluation of the same pre-compiled expression.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 5)
@Fork(value = 2, warmups = 1)
@Threads(1)
public class ParserNGWars {

    // The expression to benchmark
    private static final String EXPRESSION = "(sin(3) + cos(4 - sin(2))) ^ (-2)";
    private static final String EXPRESSION2 = "sin(3)+cos(5)-2.718281828459045^2";
    private static final String EXPRESSION3 = "((12+5)*3 - (45/9))^2";
    private static final String EXPRESSION4 = "5*sin(3+2)/(4*3-2)";
    private static final String EXPRESSION5 = "(1+1)*(1+2)*(3+4)*(8+9)*(6-1)*(4^3.14159265357)-(3+2)^1.8";

    // Pre-compiled instances (initialized in @Setup)
    private MathExpression parserNg;
    private Expression exp4j; 

    @Setup(Level.Trial)
    public void setup() {
        // ParserNG - compile once
        parserNg = new MathExpression(EXPRESSION); 

        // Exp4J - build once
        exp4j = new ExpressionBuilder(EXPRESSION).build();

 
    }

    // === ParserNG Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void parserNg(Blackhole blackhole) {
        double result = parserNg.solveGeneric().scalar;
        blackhole.consume(result);
    }

    // === Exp4J Benchmark ===
    @org.openjdk.jmh.annotations.Benchmark
    public void exp4j(Blackhole blackhole) {
        double result = exp4j.evaluate();
        blackhole.consume(result);
    }
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
                .jvmArgs("-Xms2g", "-Xmx2g") // tune heap if needed
                .build();

        new Runner(opt).run();
    }
}