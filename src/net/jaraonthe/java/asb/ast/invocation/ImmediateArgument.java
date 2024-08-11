package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

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
    
    @Override
    public String toString()
    {
        return this.immediate.toString();
    }
}
