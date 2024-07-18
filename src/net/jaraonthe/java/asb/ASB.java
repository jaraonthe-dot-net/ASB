package net.jaraonthe.java.asb;

import net.jaraonthe.java.asb.parse.SourceFile;
import net.jaraonthe.java.asb.parse.Token;
import net.jaraonthe.java.asb.parse.Tokenizer;

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
	    
	    try {
	        
    	    SourceFile file = new SourceFile(args[0]);
    	    Tokenizer tokenizer = new Tokenizer(file);
    	    
    	    Token next;
    	    while ((next = tokenizer.next()) != null) {
    	        System.out.print(next);
    	        System.out.print("\t|  ");
    	        System.out.println(next.origin.getContent());
    	    }
	    
	    } catch (Exception e) {
	        //System.out.println(e);
            e.printStackTrace();
	    }
	}
}
