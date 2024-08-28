package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &length} built-in function.<br>
 * 
 * {@code &length dstLength, srcRegister}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Length implements Interpretable
{
    /**
     * Creates a {@code &length} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&length", false);
        
        // &length dstLength, srcRegister
        function.addParameterByType(Variable.Type.REGISTER, "dst");
        function.addCommandSymbols(",");
        function.addParameterByType(Variable.Type.REGISTER, "src");
        
        function.setInterpretable(new Length());
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        BigInteger length = BigInteger.valueOf(context.frame.getNumericValue("src").length);
        
        NumericValue dst = context.frame.getNumericValue("dst");
        if (length.bitLength() > dst.length) {
            throw new RuntimeError(
                "Cannot store result of &length in " + dst.getReferencedName()
                + " as the result value is too big"
            );
        }
        
        dst.write(length, context);
    }
}
