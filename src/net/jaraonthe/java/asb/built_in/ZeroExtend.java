package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &zero_extend} built-in function.<br>
 * 
 * {@code &zero_extend dstRegister, srcRegister}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ZeroExtend implements Interpretable
{
    /**
     * Creates a {@code &zero_extend} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&zero_extend", false);
        
        // &zero_extend dstRegister, srcRegister
        function.addParameterByType(Parameter.Type.REGISTER, "dst");
        function.addCommandSymbols(",");
        function.addParameterByType(Parameter.Type.REGISTER, "src");
        
        function.setInterpretable(new ZeroExtend());
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        NumericValue src = BuiltInFunction.getNumericValue("src", context.frame);
        NumericValue dst = BuiltInFunction.getNumericValue("dst", context.frame);
        if (src.length > dst.length) {
            throw new ConstraintException(
                "Cannot &zero_extend from bigger variable " + src.getReferencedName()
                + " to smaller variable " + dst.getReferencedName()
            );
        }
        
        dst.write(src.read(context), context);
    }
}
