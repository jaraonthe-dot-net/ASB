package net.jaraonthe.java.asb.exception;

/**
 * May be used when some sort of (internal) constraint is violated, which should
 * lead to a {@link ParseError}, but we don't want to throw ParseErrors from
 * the current class (because it is not considered part of the Parser).<br>
 * 
 * Usually the Parser will replace this with a ParseError exception.
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
