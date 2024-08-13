package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Interpretable;

/**
 * The {@code &sign_extend} built-in function.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class SignExtend implements Interpretable
{
    // TODO interpret() once designed
    
    
    /**
     * Creates a {@code &sign_extend} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&sign_extend", false);
        
        // TODO need more advanced length matchers to properly define the parameters
        // &sign_extend dstRegister, srcRegister
        function.addParameter(new Variable(Variable.Type.REGISTER, "dstRegister", 32));
        function.addCommandSymbols(",");
        function.addParameter(new Variable(Variable.Type.REGISTER, "srcRegister", 12));
        
        function.setInterpretable(new SignExtend());
        return function;
    }
}
