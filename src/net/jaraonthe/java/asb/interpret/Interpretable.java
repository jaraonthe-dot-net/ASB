package net.jaraonthe.java.asb.interpret;

import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;

/**
 * Something that can be executed by the Interpreter.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public interface Interpretable
{
    /**
     * Interprets this entity.
     * 
     * @param context
     * 
     * @throws ConstraintException An error without Origin information (that
     *                             information should be added somewhere)
     * @throws RuntimeError        An error that can be directly displayed to
     *                             the user
     */
    public void interpret(Context context) throws ConstraintException, RuntimeError;
}
