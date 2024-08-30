package net.jaraonthe.java.asb.exception;

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
     * @return A printable representation of this error message intended for
     *         the user
     */
    public String getUserReadable()
    {
        return "Error: " + this.getMessage();
    }
}
