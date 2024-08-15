package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;

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
     * @param immediate The numeric immediate value this contains
     */
    public ImmediateArgument(BigInteger immediate)
    {
        this.immediate = immediate;
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
    public Variable.Type getVariableType()
    {
        return Variable.Type.IMMEDIATE;
    }
    
    @Override
    public String toString()
    {
        return this.immediate.toString();
    }
}
