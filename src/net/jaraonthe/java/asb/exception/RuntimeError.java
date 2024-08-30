package net.jaraonthe.java.asb.exception;

/**
 * Used for those constraint errors that can only be picked up during
 * interpretation. 
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class RuntimeError extends UserError
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public RuntimeError(String message)
    {
        super(message);
    }

    @Override
    public String getUserReadable()
    {
        return "Runtime Error: " + this.getMessage();
    }
}
