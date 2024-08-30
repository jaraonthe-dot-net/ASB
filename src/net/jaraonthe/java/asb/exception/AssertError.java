package net.jaraonthe.java.asb.exception;

import net.jaraonthe.java.asb.built_in.Assert;

/**
 * Used when an {@code &assert} check fails.
 *
 * @see Assert
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class AssertError extends RuntimeError
{
    private static final long serialVersionUID = 1L;
    
    /**
     * @param message
     */
    public AssertError(String message)
    {
        super(message);
    }

    @Override
    protected String getTitle()
    {
        return "Assert Error";
    }
}
