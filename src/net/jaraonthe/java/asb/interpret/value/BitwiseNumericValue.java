package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A numeric value derived from bitwise access to another value.
 * 
 * Every read and write access is translated onto the base value.
 * 
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class BitwiseNumericValue extends NumericValue
{
    /**
     * The Value accessed via this.
     */
    public final NumericValue accessed;
    
    /**
     * Start position of bitwise access.
     */
    public final int fromPosition;
    
    /**
     * End position of bitwise access.
     */
    public final int toPosition;
    
    /**
     * @param variable     The Variable which this Value is assigned to
     * @param access       The Variable that is accessed bitwise
     * @param fromPosition Start position of bitwise access
     * @param toPosition   End position of bitwise access
     */
    public BitwiseNumericValue(Variable variable, NumericValue accessed, int fromPosition, int toPosition)
    {
        super(variable, Math.abs(fromPosition - toPosition) + 1);
        this.accessed     = accessed;
        this.fromPosition = fromPosition;
        this.toPosition   = toPosition;

        if (
            !Constraints.isValidPosition(fromPosition)
            || fromPosition >= accessed.length
            || !Constraints.isValidPosition(toPosition)
            || toPosition >= accessed.length
        ) {
            throw new IllegalArgumentException(
                "Given position " + this.position2String() + " is not valid for "
                + accessed.getReferencedName()
            );
        }
    }
    
    @Override
    public BigInteger read(Context context) throws RuntimeError
    {
        BigInteger baseValue = this.accessed.read(context);
        byte[] bitwise = new byte[this.length / 8 + 1];
        
        int pos = this.toPosition;
        int step;
        if (this.fromPosition > this.toPosition) {
            step = 1;
        } else {
            step = -1;
        }
        // Read from toPosition into LSB up to fromPosition into MSB
        for (int i = 0; i < this.length; i++) {
            // TODO Consider optimizing so that not every bit is individually tested (same below)
            //      - but keep in mind that this implementation also works with
            //        negative immediate values out of the box!
            if (baseValue.testBit(pos)) {
                bitwise[bitwise.length - i / 8 - 1] |= 1 << (i % 8);
            }
            pos += step;
        }
        
        return new BigInteger(bitwise);
    }

    @Override
    public void write(BigInteger value, Context context) throws RuntimeError
    {
        this.checkValueLength(value);
        
        // Modifying parts of the value & writing it back

        byte[] stored = this.accessed.read(context).toByteArray();
        int requiredBytes = (Math.max(this.fromPosition, this.toPosition) + 1) / 8 + 1;
        if (requiredBytes > stored.length) {
            byte[] enlarged = new byte[requiredBytes];
            System.arraycopy(stored, 0, enlarged, enlarged.length - stored.length, stored.length);
            stored = enlarged;
        }
        
        int pos = this.toPosition;
        int step;
        if (this.fromPosition > this.toPosition) {
            step = 1;
        } else {
            step = -1;
        }
        // Read from LSB into toPosition up to MSB into fromPosition
        for (int i = 0; i < this.length; i++) {
            int mask = 1 << (pos % 8);
            if (value.testBit(i)) {
                stored[stored.length - pos / 8 - 1] |= mask;
            } else {
                stored[stored.length - pos / 8 - 1] &= ~mask;
            }
            pos += step;
        }
        
        this.accessed.write(new BigInteger(stored), context);
    }

    @Override
    public String toString()
    {
        return this.accessed.toString() + this.position2String();
    }

    @Override
    public String getReferencedName()
    {
        return this.accessed.getReferencedName() + this.position2String();
    }
    
    private String position2String()
    {
        String position = "'" + this.fromPosition;
        if (this.fromPosition != this.toPosition) {
            position += ":" + this.toPosition;
        }
        return position;
    }
}
