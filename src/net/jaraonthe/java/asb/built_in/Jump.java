package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
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
        function.addParameter(new Variable(
            Variable.Type.LABEL,
            "label",
            Variable.LOCAL_LABEL_LENGTH,
            true
        ));
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        Jump.jump(context);
    }
    
    /**
     * Jumps locally.<br>
     * 
     * Context contains:<br>
     * - the frame within which to jump<br>
     * - LabelValue called "label" that contains the position to which to jump
     * 
     * @param context
     * @throws RuntimeError
     */
    public static void jump(Context context) throws RuntimeError
    {
        LabelValue label           = (LabelValue) context.frame.getValue("label");
        label.frame.programCounter = label.argument.getLabelPosition();
    }
}
