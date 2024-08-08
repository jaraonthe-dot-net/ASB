package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.VariableLike;

/**
 * An invocation argument that refers to a register or parameter or local
 * variable.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class RegisterArgument extends Argument
{
    /**
     * The register or parameter or local variable this refers to
     */
    public final VariableLike register;
    
    // TODO bitwise access details

    /**
     * @param register The register or parameter or local variable this argument
     *                 refers to
     */
    public RegisterArgument(VariableLike register)
    {
        this.register = register;
    }
}
