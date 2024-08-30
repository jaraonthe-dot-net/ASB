package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

/**
 * The {@code &normalize} built-in function.<br>
 * 
 * {@code &normalize variable}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Normalize implements Interpretable
{    
    /**
     * Creates a {@code &normalize} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&normalize", false);
        
        // &normalize register
        function.addParameterByType(Variable.Type.REGISTER, "variable");
        
        function.setInterpretable(new Normalize());
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue operand = BuiltInFunction.getNumericValue("variable", context.frame);
        if (!(operand.variable instanceof Variable)) {
            // Registers are never negative
            return;
        }
        
        BigInteger value = operand.read(context);
        operand.write(NumericValueStore.normalizeBigInteger(value, operand.length), context);
    }
}
