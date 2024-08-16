package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Variable;

/**
 * An argument used in an invocation (after invocation has been resolved).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Argument
{
    /**
     * @return The Variable type corresponding to this argument.
     */
    abstract public Variable.Type getVariableType();
}
