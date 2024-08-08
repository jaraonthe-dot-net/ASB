package net.jaraonthe.java.asb.ast.invocation;

/**
 * An invocation argument that contains a literal string value.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class StringArgument extends Argument
{
    /**
     * The string value this contains
     */
    public final String string;

    /**
     * @param string The string value this contains
     */
    public StringArgument(String string)
    {
        this.string = string;
    }
}
