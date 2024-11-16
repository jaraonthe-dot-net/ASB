package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A local variable. Has a name, length (or length variable), and optionally
 * some groups (which control which commands this variable can be used with).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class LocalVariable extends RegisterLike
{
    /**
     * If this is not null, then the referenced Variable is the source for this
     * Local Variable's length setting (which is determined dynamically). In
     * this case {@link Variable#minLength minLength} and
     * {@link Variable#maxLength maxLength} are meaningless.<br>
     */
    public final Variable lengthVariable;
    
    /**
     * @param name
     * @param length length in bits
     */
    public LocalVariable(String name, int length) {
        super(name, length);
        this.lengthVariable = null;
    
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid local variable " + name + " length. Given value is: " + length
            );
        }
    }
    
    /**
     * @param name
     * @param lengthVariable The value of this variable is used as the length of
     *        the local variable.
     */
    public LocalVariable(String name, Variable lengthVariable) {
        super(name, 0);
        this.lengthVariable = lengthVariable;
        
        if (!lengthVariable.isNumeric()) {
            throw new IllegalArgumentException(
                "Cannot use " + lengthVariable + " parameter as length register"
            );
        }
    }
}
