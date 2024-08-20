package net.jaraonthe.java.asb.interpret;

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
     * @throws RuntimeError
     */
    public void interpret(Context context) throws RuntimeError;
}
