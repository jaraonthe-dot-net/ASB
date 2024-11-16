package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.LabelValue;

/**
 * The {@code &jump} built-in function.<br>
 * 
 * {@code &jump label}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Jump implements Interpretable
{
    /**
     * Creates a {@code &jump} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&jump", false);
        
        Jump.addLabelParameter(function);
        
        function.setInterpretable(new Jump());
        return function;
    }
    
    /**
     * Adds a LABEL parameter called "label" to the given function
     * 
     * @param function
     */
    public static void addLabelParameter(BuiltInFunction function)
    {
        function.addParameterByType(Parameter.Type.LABEL, "label");
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        Jump.jump(context);
    }
    
    /**
     * Jumps locally.<br>
     * 
     * Context must contain:<br>
     * - the frame within which to jump<br>
     * - LabelValue called "label" that contains the position to which to jump
     * 
     * @param context
     */
    public static void jump(Context context)
    {
        LabelValue label           = (LabelValue) BuiltInFunction.getValue("label", context.frame);
        label.frame.programCounter = label.argument.getLabelPosition();
    }
}
