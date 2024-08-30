package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

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
    protected final BuiltInFunction.OperandType src;
    
    
    /**
     * @param src Selects the function variant (via the src type)
     */
    private Not(BuiltInFunction.OperandType src)
    {
        this.src = src;
    }
    
    /**
     * Creates a {@code &not} built-in function with the given operand variant.
     * 
     * @param src Selects the function variant (via the src type)
     * @return
     */
    public static BuiltInFunction create(BuiltInFunction.OperandType src)
    {
        BuiltInFunction function = new BuiltInFunction("&not", false);

        function.addParameterByType(Variable.Type.REGISTER, "dst");
        function.addCommandSymbols(",");
        function.addParameterByType(src, "src");
        
        function.setInterpretable(new Not(src));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        NumericValue src = BuiltInFunction.getNumericValue("src", context.frame);
        NumericValue dst = BuiltInFunction.getNumericValue("dst", context.frame);

        BigInteger srcValue = src.read(context);
        // Check lengths
        if (
            (this.src == BuiltInFunction.OperandType.IMMEDIATE) ?
                (NumericValue.bitLength(srcValue) > dst.length)
                : (src.length != dst.length)
        ) {
            throw new ConstraintException(
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
