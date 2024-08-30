package net.jaraonthe.java.asb.exception;

/**
 * May be used when some sort of (internal) constraint is violated, which should
 * lead to a {@link ParseError} or {@link RuntimeError}, but we don't know the
 * Origin that should be mentioned in the error message. Thus this exception is
 * thrown and somewhere else will be replaced with an *Error exception with
 * Origin information.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ConstraintException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public ConstraintException(String message)
    {
        super(message);
    }
}
