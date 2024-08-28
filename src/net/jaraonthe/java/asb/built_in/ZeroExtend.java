package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
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
        function.addParameterByType(Variable.Type.REGISTER, "dst");
        function.addCommandSymbols(",");
        function.addParameterByType(Variable.Type.REGISTER, "src");
        
        function.setInterpretable(new ZeroExtend());
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src = context.frame.getNumericValue("src");
        NumericValue dst = context.frame.getNumericValue("dst");
        if (src.length > dst.length) {
            throw new RuntimeError(
                "Cannot &zero_extend from bigger variable " + src.getReferencedName()
                + " to smaller variable " + dst.getReferencedName()
            );
        }
        
        dst.write(src.read(context), context);
    }
}
