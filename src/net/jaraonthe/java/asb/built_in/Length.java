package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Interpretable;
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
    // TODO interpret() once designed
    
    
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
