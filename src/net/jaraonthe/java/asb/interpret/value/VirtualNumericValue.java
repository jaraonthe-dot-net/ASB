package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.LocalVariable;
import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.ast.variable.VirtualRegister;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Frame;

/**
 * A numeric value as represented by a VirtualRegister.<br>
 * 
 * When reading or writing this value, the virtual register's getter or setter
 * are invoked. 
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class VirtualNumericValue extends NumericValue
{
    /**
     * Same as {@link Value#variable}, but with a more convenient type.
     */
    public final VirtualRegister register;
    
    /**
     * The value stored in this register. Null if this virtual register has no
     * store.
     */
    private final NumericValueStore store;
    
    /**
     * Used by {@link #toString()}.
     */
    private BigInteger lastRead = null;
    
    /**
     * Creates a new numeric value using a virtual register.
     * 
     * @param register The virtual register this value represents.
     */
    public VirtualNumericValue(VirtualRegister register)
    {
        super(register, register.getLength());
        this.register = register;
        
        if (register.hasStore()) {
            // Modeling store as a (persistent) local variable
            this.store = new NumericValueStore(new LocalVariable(
                "store",
                register.getStoreLength()
            ));
        } else {
            this.store = null;
        }
    }
    
    @Override
    public BigInteger read(Context context) throws RuntimeError
    {
        Frame newFrame = new Frame(context.frame.getRootParentFrame());
        NumericValueStore out = new NumericValueStore(new Parameter(
            Parameter.Type.REGISTER,
            "out",
            this.register.getLength()
        ));
        newFrame.addValue(out);
        
        if (this.store != null) {
            newFrame.addValue(this.store);
        }
        
        this.register.getGetterImplementation().interpret(context.withFrame(newFrame));
        
        return this.lastRead = out.read(context);
    }

    @Override
    public void write(BigInteger value, Context context) throws RuntimeError
    {
        this.checkValueLength(value);
        
        Frame newFrame = new Frame(context.frame.getRootParentFrame());
        NumericValueStore in = new NumericValueStore(new Parameter(
            Parameter.Type.REGISTER,
            "in",
            this.register.getLength()
        ));
        in.write(value, context);
        newFrame.addValue(in);
        
        if (this.store != null) {
            newFrame.addValue(this.store);
        }
        
        this.register.getSetterImplementation().interpret(context.withFrame(newFrame));
    }

    @Override
    public String toString()
    {
        // Using the last read value is a hack-ish solution. It's done here
        // because we don't have necessary Context to invoke the getter
        if (this.lastRead == null) {
            return "0";
        }
        
        return this.lastRead.toString();
    }
}
