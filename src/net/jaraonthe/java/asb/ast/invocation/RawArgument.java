package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Parameter;
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
     * The register or parameter or local variable this may refer to. Must not
     * be null.<br>
     * 
     * This is stored here so that the resolving can already take place at the
     * location where the Variable is used, even though resolving the invoked
     * command may happen later; this way only Registers and Variables that
     * exist at the location of usage are taken into account.
     */
    public final Variable potentialVariable;

    /**
     * @param name
     * @param potentialVariable Must not be null
     */
    public RawArgument(String name, Variable potentialVariable)
    {
        this.name              = name;
        this.potentialVariable = potentialVariable;
    }

    @Override
    public Parameter.Type getParameterType()
    {
        // This should be fine as signatureMarker is the same for both REGISTER
        // and LABEL
        return Parameter.Type.REGISTER;
    }
    
    @Override
    public String toString()
    {
        return this.name;
    }
}
