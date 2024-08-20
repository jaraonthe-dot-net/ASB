package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.VariableLike;
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
     */
    abstract public BigInteger read(Context context);

    /**
     * Overwrites the content of this value.
     * 
     * @param value
     * @param context
     */
    abstract public void write(BigInteger value, Context context);
    
    /**
     * Instead of containing their own content, NumericValues may refer to
     * a different value instead (e.g. virtual register).
     * 
     * This allows to retrieve that referenced value.
     * 
     * @return
     */
    // TODO decide if the referenced values length is the same as for this, if
    //      its read() and write() behave the same as for this - essentially
    //      decide if bitwise access gives itself as referenced or the value it
    //      operates on. This also determines what getReferenced() can be used
    //      for.
    abstract public NumericValueStore getReferenced();
}
