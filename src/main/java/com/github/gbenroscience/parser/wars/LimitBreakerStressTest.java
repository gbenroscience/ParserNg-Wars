/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.github.gbenroscience.parser.wars;

/**
 *
 * @author GBEMIRO
 */
import com.github.gbenroscience.parser.MathExpression;
import com.github.gbenroscience.parser.turbo.tools.ScalarTurboEvaluator;
import org.codehaus.janino.ExpressionEvaluator;

import java.util.Arrays;

public class LimitBreakerStressTest {

    public static void main(String[] args) {
        int largeVarCount = 500; // Well beyond the JVM 255-slot limit
        
        // 1. Generate a massive linear expression: x0 + x1 + ... + x499
        StringBuilder sb = new StringBuilder("x0");
        String[] varNames = new String[largeVarCount];
        Class<?>[] varTypes = new Class<?>[largeVarCount];
        double[] values = new double[largeVarCount];
        
        varNames[0] = "x0";
        varTypes[0] = double.class;
        values[0] = 1.0;

        for (int i = 1; i < largeVarCount; i++) {
            sb.append("+x").append(i);
            varNames[i] = "x" + i;
            varTypes[i] = double.class;
            values[i] = 1.0;
        }
        String bigExpr = sb.toString();

        System.out.println("--- Testing with " + largeVarCount + " variables ---");

        // 2. Attempt Janino Compilation
        try {
            System.out.print("Testing Janino... ");
            ExpressionEvaluator ee = new ExpressionEvaluator(
                bigExpr,
                double.class,
                varNames,
                varTypes
            );
            // Convert double[] to Object[] for Janino
            Object[] params = Arrays.stream(values).boxed().toArray();
            System.out.println("Result: " + ee.evaluate(params));
        } catch (Exception | LinkageError e) {
            System.err.println("\n[JANINO FAILED]: " + e.getMessage());
            System.err.println("Reason: Exceeded JVM Method Parameter Limit (255 slots).");
        }

        System.out.println("--------------------------------------------------");

        // 3. Attempt ParserNG Compilation (Array-Based)
        try {
            System.out.print("Testing ParserNG (Array-Based)... ");
            MathExpression me = new MathExpression(bigExpr, false);
            // Force registration of all variables
            for (String name : varNames) { me.getVariable(name); }
            
            // Compile using the stable Array-Based strategy
            var evaluator = new ScalarTurboEvaluator(me, false).compile();
            
            double result = (double) evaluator.applyScalar(values);
            System.out.println("SUCCESS!");
            System.out.println("Result: " + result);
        } catch (Throwable t) {
            System.err.println("\n[PARSERNG FAILED]: " + t.getMessage());
        }
    }
}