package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &not} built-in function.<br>
 * 
 * {@code &not dstRegister, srcImm};<br>
 * {@code &not dstRegister, srcRegister};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Not implements Interpretable
{
    public enum Operand
    {
        REGISTER,
        IMMEDIATE,
    }
    
    protected final Not.Operand operand;
    
    
    /**
     * @param operand Selects the function variant (via the Operand type)
     */
    private Not(Not.Operand operand)
    {
        this.operand = operand;
    }
    
    /**
     * Creates a {@code &not} built-in function with the given operand variant.
     * 
     * @param operand Selects the function variant (via the Operand type)
     * @return
     */
    public static BuiltInFunction create(Not.Operand operand)
    {
        BuiltInFunction function = new BuiltInFunction("&not", false);

        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "dst",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        function.addCommandSymbols(",");
        
        switch (operand) {
            case REGISTER:
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        
            case IMMEDIATE:
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src",
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Not(operand));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src = context.frame.getNumericValue("src");
        NumericValue dst = context.frame.getNumericValue("dst");

        BigInteger srcValue = src.read(context);
        // Check lengths
        if (
            (this.operand == Not.Operand.IMMEDIATE) ?
                (NumericValue.bitLength(srcValue) > dst.length)
                : (src.length != dst.length)
        ) {
            throw new RuntimeError(
                "Cannot &not from variable " + src.getReferencedName() + " to "
                + dst.getReferencedName() + " as they do not have the same length"
            );
        }
        
        dst.write(
            NumericValueStore.normalizeBigInteger(srcValue.not(), dst.length),
            context
        );
    }
}
