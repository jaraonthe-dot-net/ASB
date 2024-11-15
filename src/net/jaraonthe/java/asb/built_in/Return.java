package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;

/**
 * The {@code &return} built-in function.<br>
 * 
 * {@code &jump}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Return implements Interpretable
{
    /**
     * Creates a {@code &return} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&return", false, true);
        
        function.setInterpretable(new Return());
        return function;
    }
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        // Due to useCallerFrame, this operates on the caller's program counter
        context.frame.programCounter = -1;
    }
}
