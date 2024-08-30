package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;
import net.jaraonthe.java.asb.parse.Constraints;
import net.jaraonthe.java.asb.parse.Origin;

/**
 * A special kind of invocation: This initializes a local variable if it doesn't
 * exist yet.<br>
 * 
 * Local variable definitions (.variable) are parsed into this in order to
 * support dynamic length. Due to jumps this invocation may be executed more
 * than once, but a local variable can only be initialized once; hence once the
 * local variable exists (in the current Frame), this does nothing.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class LocalVariableInitialization implements Invocation
{
    public final Variable localVariable;
    
    private Origin origin;
    
    
    /**
     * @param localVariable
     */
    public LocalVariableInitialization(Variable localVariable)
    {
        if (localVariable.type != Variable.Type.LOCAL_VARIABLE) {
            throw new IllegalArgumentException("localVariable is not of type LOCAL_VARIABLE");
        }
        
        this.localVariable = localVariable;
    }
    

    @Override
    public Origin getOrigin()
    {
        return this.origin;
    }
    
    /**
     * Sets this invocation's origin.
     * 
     * @param origin
     * @return Fluent interface
     */
    public LocalVariableInitialization setOrigin(Origin origin)
    {
        this.origin = origin;
        return this;
    }

    @Override
    public boolean isResolved()
    {
        return true;
    }

    @Override
    public Invocation resolve(AST ast, Implementation implementation)
    {
        // Nothing
        return this;
    }

    @Override
    public Invocation resolveLabelNames(AST ast, Implementation implementation)
    {
        // Nothing
        return this;
    }
    
    @Override
    public String toString()
    {
        return this.localVariable.toString();
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        if (!context.frame.valueExistsLocally(this.localVariable.name)) {
            
            int length;
            if (this.localVariable.maxLength != 0) {
                length = this.localVariable.maxLength;
            } else {
                // dynamic length
                BigInteger value;
                try {
                    value = context.frame.getNumericValue(this.localVariable.lengthRegister.name)
                        .read(context);
                } catch (ConstraintException e) {
                    throw new RuntimeError(e.getMessage() + " at " + this.getOrigin());
                }
                try {
                    length = value.intValueExact();
                    if (!Constraints.isValidLength(length)) {
                        throw new ArithmeticException("dummy");
                    }
                    
                } catch (ArithmeticException e) {
                    throw new RuntimeError(
                        "Value stored in " + this.localVariable.lengthRegister.name
                        + " is used as a local variable's length, but is too "
                        + (value.signum() > 0 ? "big" : "small") + " (is " + value + ") at "
                        + this.getOrigin()
                    );
                }
            }
            
            context.frame.addValue(new NumericValueStore(localVariable, length));
        }
    }
}
