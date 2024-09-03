package net.jaraonthe.java.asb.exception;

import net.jaraonthe.java.asb.Print;
import net.jaraonthe.java.asb.Settings;

/**
 * Represents an error that is caused by user input.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class UserError extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public UserError(String message)
    {
        super(message);
    }
    
    /**
     * @return A title for this type of error
     */
    protected String getTitle()
    {
        return "Error";
    }
    
    /**
     * Does a user-readable print in color, honoring settings.
     * 
     * @param settings May be null
     */
    public void print(Settings settings)
    {
        if (settings != null && settings.devMode()) {
            // Print all the technical details
            this.printStackTrace();
            return;
        }
        
        Print.printBoldWithColor(this.getTitle(), Print.Color.RED, settings);
        Print.printlnWithColor(": " + this.getMessage(), Print.Color.RED, settings);
    }
}
