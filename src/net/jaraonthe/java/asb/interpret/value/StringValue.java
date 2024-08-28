package net.jaraonthe.java.asb.interpret.value;

import net.jaraonthe.java.asb.ast.invocation.StringArgument;
import net.jaraonthe.java.asb.ast.variable.Variable;

/**
 * The value of a string parameter.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class StringValue extends Value
{
    /**
     * The text content of this value.
     */
    public final String value;
    
    /**
     * @param variable the Variable which this Value is assigned to
     * @param argument the argument that is used to fill this parameter
     */
    public StringValue(Variable variable, StringArgument argument)
    {
        super(variable);
        this.value = argument.string;
    }
    
    @Override
    public String toString()
    {
        return this.value;
    }
}
