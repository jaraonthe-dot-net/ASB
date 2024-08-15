package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Variable;

/**
 * A raw invocation argument. This refers to either a Variable or a Label by
 * name, but which is not yet clear. When the invocation is resolved this will
 * be replaced with the actual Argument object.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class RawArgument extends Argument
{
    /**
     * The name which points to either a Variable or a Label.
     */
    public final String name;

    /**
     * @param name
     */
    public RawArgument(String name)
    {
        this.name = name;
    }

    @Override
    public Variable.Type getVariableType()
    {
        // This should be fine as signatureMarker is the same for both REGISTER
        // and LABEL
        return Variable.Type.REGISTER;
    }
    
    @Override
    public String toString()
    {
        return this.name;
    }
}
