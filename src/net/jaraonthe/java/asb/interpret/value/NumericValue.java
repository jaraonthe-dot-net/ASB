package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * A numeric value (as stored in a register or local variable), as opposed to
 * string or label values.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class NumericValue extends Value
{
    /**
     * The effective length of this value.
     */
    public final int length;
    
    /**
     * @param variable the Variable which this Value is assigned to
     * @param length   the effective length of this value
     */
    protected NumericValue(VariableLike variable, int length)
    {
        super(variable);
        
        if (!variable.isNumeric()) {
            throw new IllegalArgumentException(
                "Cannot use non-numeric Variable " + variable + " for a numeric value"
            );
        }
        this.length = length;
    }

    /**
     * Reads the content of this value.
     * 
     * @param context
     * @return
     * @throws RuntimeError
     */
    abstract public BigInteger read(Context context) throws RuntimeError;

    /**
     * Overwrites the content of this value.
     * 
     * @param value
     * @param context
     * 
     * @throws RuntimeError
     */
    abstract public void write(BigInteger value, Context context) throws RuntimeError;
    
    /**
     * Returns the bit length of the given BigInteger.<br>
     * 
     * Opposed to {@link BigInteger#bitLength()}, this also includes the sign
     * bit in the returned length.
     * 
     * @param value
     * @return
     */
    public static int bitLength(BigInteger value)
    {
        // This works for normalized as well as (possibly negative) immediate values
        return value.bitLength() + (value.signum() < 0 ? 1 : 0);
    }
    
    /**
     * Instead of containing their own content, NumericValues may refer to
     * a different value instead (e.g. register alias).
     * 
     * This allows to retrieve that referenced value.<br>
     * 
     * The returned value has the same length as this, and there is no
     * behavioral difference between calling {@link #read()} or {@link #write()}
     * on this or the returned value.
     * 
     * @return
     */
    public NumericValue getReferenced()
    {
        return this;
    }
    
    /**
     * Provides a variable name that represents the value used here. This uses
     * the name associated with the referenced value as this is usually more
     * meaningfull as e.g. the name of a command parameter.<br>
     * 
     * Subclasses may overwrite this to provide something more meaningful.
     * 
     * @return
     */
    public String getReferencedName()
    {
        return this.getReferenced().variable.name;
    }
    
    /**
     * Checks that given value fits the length of this NumericValue.
     * 
     * @param value
     * @throws IllegalArgumentException if value is too big
     */
    protected void checkValueLength(BigInteger value)
    {
        if (NumericValue.bitLength(value) > this.length) {
            throw new IllegalArgumentException(
                "Value is to big for " + this.getReferencedName() + ", is " + value
            );
        }
    }
}
