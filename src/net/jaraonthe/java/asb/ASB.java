package net.jaraonthe.java.asb;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.exception.UserError;
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
     * The current application version.
     */
    public static final String VERSION = "0.1-dev";
    
    public static final String HELP_TEXT =
        "Usage:\n"
        + "    asb <file> [<file>...] [options]\n"
        + "            given files will be parsed as ASB source code in the given order\n"
        + "\n"
        + "Available options:\n"
        + "    -C\n"
        + "    --no-color\n"
        + "            to switch off colorful output\n"
        + "    -i <file>\n"
        + "    --include <file>\n"
        + "            includes the given file before parsing the main files\n"
        + "\n"
        + "Other usages:\n"
        + "    asb -h\n"
        + "    asb --help\n"
        + "            to display help\n"
        + "    asb -v\n"
        + "    asb --version\n"
        + "            to display the application version\n"
        + "    asb --about\n"
        + "            to display credits and legal information";
    
    public static final String ABOUT_TEXT =
        "Assembler Sandbox " + ASB.VERSION + "\n"
        + "\n"
        + "Created by Jakob Rathbauer <jakob@jaraonthe.net>\n"
        + "\n"
        + "License (GPL v3):\n"
        + "\n"
        + "This program is free software: you can redistribute it and/or modify\n"
        + "it under the terms of the GNU General Public License as published by\n"
        + "the Free Software Foundation, either version 3 of the License, or\n"
        + "(at your option) any later version.\n"
        + "\n"
        + "This program is distributed in the hope that it will be useful,\n"
        + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
        + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
        + "GNU General Public License for more details.\n"
        + "\n"
        + "You should have received a copy of the GNU General Public License\n"
        + "along with this program.  If not, see <http://www.gnu.org/licenses/>.";
    
	/**
	 * @param args See the {@link #HELP_TEXT}
	 */
	public static void main(String[] args)
	{
	    Settings settings;
	    try {
	        settings = Settings.fromArgs(args);
	    } catch (UserError e) {
	        System.out.println(e.getUserReadable());
	        return;
	    }
	    
	    switch (settings.getMode()) {
            case VERSION:
                System.out.println("asb " + ASB.VERSION);
                break;
            
	        case HELP:
	            System.out.println(ASB.HELP_TEXT);
	            break;
            
	        case ABOUT:
	            System.out.println(ASB.ABOUT_TEXT);
	            break;
	            
	        case MAIN:
        	    try {
        	        if (settings.getFilePaths().isEmpty()) {
        	            throw new UserError("No file given. See asb --help");
        	        }
        	        
        	        AST ast = Parser.parse(settings.getFilePaths());
            	    Interpreter.interpret(ast, settings);
            	    
        	    } catch (UserError e) {
        	        if (settings.getDevMode()) {
        	            // Print all the technical details
        	            e.printStackTrace();
        	        } else {
        	            Print.printlnWithColor(e.getUserReadable(), Print.Color.RED, settings);
        	        }
        	        return;
        	    } catch (Exception e) {
                    e.printStackTrace();
                    return;
        	    }
    	    break;
	    }
	}
}
