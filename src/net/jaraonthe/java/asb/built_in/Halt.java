package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;

/**
 * The {@code &halt} built-in function.<br>
 * 
 * {@code &halt}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Halt implements Interpretable
{
    /**
     * Creates a {@code &halt} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&halt", true);
        
        function.setInterpretable(new Halt());
        return function;
    }
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        // Let's point to right after the last invocation in userland program
        context.frame.getRootParentFrame().programCounter = context.ast.getProgram().size();
    }
}
