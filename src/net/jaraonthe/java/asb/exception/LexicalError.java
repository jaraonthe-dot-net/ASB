package net.jaraonthe.java.asb.exception;

/**
 * Represents an error that occurred when tokenizing input. I.e. no valid Token
 * could be found due to incorrect Syntax.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class LexicalError extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public LexicalError(String message)
    {
        super(message);
    }
}
