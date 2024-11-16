package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A (global) register, as configured by the user. Has a name, length, and
 * optionally some groups (which control which commands this register can be
 * used with).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Register extends RegisterLike
{
    /**
     * @param name
     * @param length length in bits
     */
    public Register(String name, int length) {
        super(name, length);
    
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid register " + name + " length. Given value is: " + length
            );
        }
    }
}
