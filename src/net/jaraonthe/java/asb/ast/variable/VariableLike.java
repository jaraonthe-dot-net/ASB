package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * Stuff that both {@link Variable} and {@link Register} have in common.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class VariableLike
{
    public final String name;
    
    /**
     * The minimum length in bits.<br>
     * -1 if length settings are not applicable for this variable type (as
     * defined by subclasses).<br>
     * 0 if length is determined dynamically (i.e. a length register for a
     * local variable).
     */
    public final int minLength;

    /**
     * The maximum length in bits.<br>
     * -1 if length settings are not applicable for this variable type (as
     * defined by subclasses).<br>
     * 0 if length is determined dynamically (i.e. a length register for a
     * local variable).
     */
    public final int maxLength;
    
    
    /**
     * Length values are not checked here. Please do that in the subclass
     * constructor.
     * 
     * @param name
     * @param minLength
     * @param maxLength
     */
    protected VariableLike(String name, int minLength, int maxLength)
    {
        this.name      = name;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }
    
    
    /**
     * @return True if this register/variable holds a numeric value (i.e. NOT
     *         string or label).
     */
    public boolean isNumeric()
    {
        return this.minLength != -1;
    }
    
    @Override
    public String toString()
    {
        if (this.minLength != -1) {
            return this.name + "''" + this.lengthAsString();
        }
        return this.name;
    }
    
    /**
     * @return The length settings as a human-readable and technically
     *         deterministic string. Without leading "''".
     */
    public String lengthAsString()
    {
        if (this.minLength == -1) {
            return "";
        }
        if (this.minLength == this.maxLength) {
            return String.valueOf(this.minLength);
        }
        if (this.minLength == Constraints.MIN_LENGTH) {
            return "<=" + this.maxLength;
        }
        if (this.maxLength == Constraints.MAX_LENGTH) {
            return ">=" + this.minLength;
        }
        return this.minLength + ".." + this.maxLength;
    }
}
