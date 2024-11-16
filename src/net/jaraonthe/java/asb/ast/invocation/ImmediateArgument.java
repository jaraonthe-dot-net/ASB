package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Parameter;

/**
 * An invocation argument that contains an immediate value.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ImmediateArgument extends Argument
{
    /**
     * The immediate value this contains
     */
    public final BigInteger immediate;
    
    /**
     * The string representation of {@link #immediate}, as given in ASB source
     * code.
     * 
     * This is required in case it is a label argument after all.
     */
    public final String asString;

    /**
     * @param immediate The numeric immediate value this contains
     * @param asString  The string representation of immediate, as given in ASB
     *                  source code
     */
    public ImmediateArgument(BigInteger immediate, String asString)
    {
        this.immediate = immediate;
        this.asString  = asString;
    }
    
    
    /**
     * @return The minimum amount of bits required to store this argument's
     *         immediate
     */
    public int getMinLength()
    {
        return this.immediate.bitLength() + (this.immediate.signum() < 0 ? 1 : 0);
    }

    @Override
    public Parameter.Type getParameterType()
    {
        return Parameter.Type.IMMEDIATE;
    }
    
    @Override
    public String toString()
    {
        return this.immediate.toString();
    }
}
