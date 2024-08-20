package net.jaraonthe.java.asb;

import java.util.ArrayList;
import java.util.List;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.interpret.Interpreter;
import net.jaraonthe.java.asb.parse.Parser;

/**
 * The main ASB program. Parses and executes a given ASB program.
 * 
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ASB
{
	/**
	 * TODO Describe args
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
	    // TODO Experimental
	    
	    List<String> filePaths = new ArrayList<>();
	    try {
	        filePaths.add(args[0]);
	        
	        AST ast = Parser.parse(filePaths);
    	    System.out.println("Parsed successfully.");
    	    
    	    Interpreter.interpret(ast);
    	    System.out.println("Ran successfully.");
    	    
	    } catch (Exception e) {
	        //System.out.println(e);
            e.printStackTrace();
	    }
	}
}
