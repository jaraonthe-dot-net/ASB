package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &length} built-in function.<br>
 * 
 * {@code &length dstLength, srcRegister}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Length implements Interpretable
{
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
    
    
    /**
     * Creates a {@code &length} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&length", false);
        
        // &length dstLength, srcRegister
        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "dst",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        function.addCommandSymbols(",");
        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "src",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        
        function.setInterpretable(new Length());
        return function;
    }
}
