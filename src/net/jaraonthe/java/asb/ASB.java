package net.jaraonthe.java.asb;

import net.jaraonthe.java.asb.parse.Parser;
import net.jaraonthe.java.asb.parse.SourceFile;

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
	    
	    Parser parser = new Parser();
	    try {
    	    SourceFile file = new SourceFile(args[0]);
    	    
    	    parser.parseFile(file);
    	    // TODO enable once built-in functions are implemented
    	    //parser.resolveImplementationInvocations();
	    
    	    System.out.println("Successfully parsed.");
	    } catch (Exception e) {
	        //System.out.println(e);
            e.printStackTrace();
	    }
	}
}
