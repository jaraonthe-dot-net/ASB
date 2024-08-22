package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.invocation.ImmediateArgument;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * A numeric value that stores the value directly.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class NumericValueStore extends NumericValue
{
    private BigInteger value = BigInteger.ZERO;
    
    /**
     * True if this is the value of an immediate parameter. If true, then value
     * may store a negative BigInteger, otherwise value will always contain a
     * positive BigInteger (which may be the two's-complement value of a
     * negative value; but BigInteger considers it to be positive).
     */
    private final boolean isImmediate;
    
    /**
     * Creates a new numeric value. The value is initialized to 0.
     * 
     * This is usually how registers and local variables are created.
     * 
     * @param variable the Variable which this Value is assigned to
     */
    public NumericValueStore(VariableLike variable)
    {
        // TODO handle length ranges, dynamic length
        super(variable, variable.maxLength);
        
        this.isImmediate = variable instanceof Variable
            && ((Variable)variable).type == Variable.Type.IMMEDIATE;
    }
    
    /**
     * Creates a new numeric value with a specific initial value.
     * 
     * This is usually how /immediate parameter values are created.
     * 
     * @param variable The Variable which this Value is assigned to
     * @param argument The argument that is used to provide the initial value.
     *                 May be null: Initial value is 0.
     *
     * @throws RuntimeError
     */
    public NumericValueStore(VariableLike variable, ImmediateArgument argument) throws RuntimeError
    {
        this(variable);
        
        if (argument != null) {
            if (argument.getMinLength() > this.length) {
                throw new RuntimeError(
                    "Immediate " + argument.immediate + " is to large for variable "
                    + this.getReferencedName()
                );
            }
            this.value = argument.immediate;
            
            if (!this.isImmediate) {
                this.value = this.normalizeBigInteger(value);
            }
        }
    }
    
    /**
     * Normalizes the given value so that it is always a positive BigInteger.
     * 
     * I.e. a negative BigInteger is changed to a positive BigInteger containing
     * a two's-complement encoding of the negative value which is as long as
     * {@code this.length}.
     * 
     * @param value
     * @return
     */
    private BigInteger normalizeBigInteger(BigInteger value)
    {
        if (value.signum() >= 0) {
            return value;
        }
        
        /* Negative => Make positive, apply bwo's-complement
         *     - Based on: https://stackoverflow.com/a/38931124
         */
        
        // Amount of extended bytes incl. leading 0 byte to enforce positive value
        int extTotalBytes = this.length / 8 + 1;
        // Amount of content bits in the extended most significant byte
        int extLeadingByteBits = this.length % 8;
        
        // Byte arrays: MSB is in element #0 (big-endian)
        byte[] content = value.toByteArray(); // Minimum length two's-complement
        if (content.length == extTotalBytes) {
            // Extended has same amount of bytes as original
            // - cut off leading high bits
            content[0] &= (0xFF >> (8 - extLeadingByteBits));
            
            return new BigInteger(content);
        }

        // Add leading high bits to extend to this.length
        // Ensure leading byte is positive to enforce positive sign (i.e. MSB is always 0)
        
        byte[] extended = new byte[extTotalBytes];
        System.arraycopy(content, 0, extended, extended.length - content.length, content.length);
        
        // Leading high bit bytes
        for (int i = extended.length - content.length - 1; i >= 1; i--) {
            extended[i] = (byte)-1;
        }
        
        // Fill up the MSB byte up to length
        extended[0] = (byte)(0xFF >> (8 - extLeadingByteBits));

        return new BigInteger(extended);
    }
    
    @Override
    public BigInteger read(Context context)
    {
        return this.value;
    }

    @Override
    public void write(BigInteger value, Context context)
    {
        // TODO Check that length isn't too big (even when not normalized)
        //      - do we actually need to check that here, or does that never
        //        happen (because of checks somewhere else)?
        this.value = value;
        if (!this.isImmediate) {
            this.value = this.normalizeBigInteger(value);
        }
    }

    @Override
    public String toString()
    {
        return this.value.toString();
    }
}
