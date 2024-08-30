package net.jaraonthe.java.asb.exception;

/**
 * Represents an error that occurred when parsing input. I.e. language grammar
 * or semantical constraints aren't met.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ParseError extends UserError
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public ParseError(String message)
    {
        super(message);
    }

    @Override
    public String getUserReadable()
    {
        return "Parse Error: " + this.getMessage();
    }
}
