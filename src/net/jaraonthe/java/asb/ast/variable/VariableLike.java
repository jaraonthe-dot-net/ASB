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
    
    /**
     * @param group
     * @return True if this Variable has the given group assigned.
     */
    abstract public boolean hasGroup(String group);
    
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
            return this.formatLengthNumber(this.minLength);
        }
        if (this.minLength == Constraints.MIN_LENGTH) {
            return "<=" + this.formatLengthNumber(this.maxLength);
        }
        if (this.maxLength == Constraints.MAX_LENGTH) {
            return ">=" + this.formatLengthNumber(this.minLength);
        }
        return this.formatLengthNumber(this.minLength) + ".." + this.formatLengthNumber(this.maxLength);
    }
    
    /**
     * Formats the given length for human-readable output. I.e. a length of 0
     * is represented by a destrictive text.
     * 
     * @param length
     * @return
     */
    private String formatLengthNumber(int length)
    {
        if (length == 0) {
            return "(dynamic)";
        }
        return String.valueOf(length);
    }
}
