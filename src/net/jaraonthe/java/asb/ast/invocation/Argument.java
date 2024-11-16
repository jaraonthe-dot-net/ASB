package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Parameter;

/**
 * An argument used in an invocation (after invocation has been resolved).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Argument
{
    /**
     * @return The Parameter type corresponding to this argument.
     */
    abstract public Parameter.Type getParameterType();
}
