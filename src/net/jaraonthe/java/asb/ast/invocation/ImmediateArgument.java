package net.jaraonthe.java.asb.ast.invocation;

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
    // TODO use something more appropriate than String (some sort of BigInteger)
    public final String immediate;

    /**
     * @param immediate The numeric immediate value this contains
     */
    public ImmediateArgument(String immediate)
    {
        this.immediate = immediate;
    }
}
