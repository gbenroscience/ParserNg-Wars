package com.github.gbenroscience.parser.wars.individual;

import org.openjdk.jmh.runner.RunnerException;

/**
 *
 * @author GBEMIRO
 */
public class Main {

    
    
    
    public static void main(String[] args) {
        if(args.length == 0){
            args = new String[]{"-i", (ParserNGWars.EXPRESSIONS.length-1)+""};
        }
        if (args.length > 0) {
            int index = 0;
            for(String arg:args){
                if(arg.equals("-i") && com.github.gbenroscience.parser.Number.isNumber(args[index+1])){
                        ParserNGWars.index = Integer.parseInt(args[1]);
                    break;
                }
                    index++;
            }
        }
        System.out.println("BENCHMARKING EXPRESSION "+ParserNGWars.getExpression());
        try {
            FieryJanino.main(args);
            BaseJanino.main(args);
            Exp4J.main(args);
            MxParser.main(args);
            Parsii.main(args);
            NativeJava.main(args);
            Paralithic.main(args);
        } catch (RunnerException ex) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

}
