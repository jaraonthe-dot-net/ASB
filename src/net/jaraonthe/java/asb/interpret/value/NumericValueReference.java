package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * A numeric value that refers to a different numeric value instead of storing
 * the value itself.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class NumericValueReference extends NumericValue
{
    /**
     * The value this refers to.
     */
    public final NumericValue referenced;
    
    /**
     * @param variable   The Variable which this Value is assigned to
     * @param referenced The value this refers to
     */
    public NumericValueReference(VariableLike variable, NumericValue referenced)
    {
        super(variable, referenced.length);
        this.referenced = referenced.getReferenced();
    }
    
    @Override
    public BigInteger read(Context context) throws RuntimeError
    {
        return this.referenced.read(context);
    }

    @Override
    public void write(BigInteger value, Context context) throws RuntimeError
    {
        this.referenced.write(value, context);;
    }

    @Override
    public NumericValue getReferenced()
    {
        return this.referenced;
    }
    
    @Override
    public String toString()
    {
        return this.referenced.toString();
    }
}
