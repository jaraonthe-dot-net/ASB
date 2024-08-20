package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &print} and {@code &println} built-in functions.
 * 
 * These functions can be invoked in userland as well.<br>
 * 
 * {@code &print register};<br>
 * {@code &print immediate};<br>
 * {@code &print string};<br>
 * {@code &println register};<br>
 * {@code &println immediate};<br>
 * {@code &println string};<br>
 * {@code &println};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Print implements Interpretable
{
    public enum Type
    {
        PRINT  ("&print"),
        PRINTLN("&println");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    
    public enum Operand
    {
        REGISTER,
        IMMEDIATE,
        STRING,
        NONE,
    }
    
    private final Print.Type type;
    private final Print.Operand operand;
    
    /**
     * @param type    Selects the actual function
     * @param operand Selects the function variant (via the Operand type)
     */
    private Print(Print.Type type, Print.Operand operands)
    {
        this.type    = type;
        this.operand = operands;
    }

    
    @Override
    public void interpret(Context context)
    {
        if (this.operand != Print.Operand.NONE) {
            System.out.print(context.frame.getValue("parameter"));
        }
        if (this.type == Print.Type.PRINTLN) {
            System.out.println();
        }
    }
    
    
    /**
     * Creates a {@code &print} or {@code &println} built-in function with the
     * given operand variant.
     * 
     * @param type    Selects the actual function
     * @param operand Selects the function variant (via the Operand type)
     * 
     * @return
     */
    public static BuiltInFunction create(Print.Type type, Print.Operand operand)
    {
        if (operand == Print.Operand.NONE && type != Print.Type.PRINTLN) {
            throw new IllegalArgumentException(
                "Cannot create &print function without operand"
            );
        }
        
        BuiltInFunction function = new BuiltInFunction(type.functionName, true);

        switch (operand) {
            case REGISTER:
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "parameter",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case IMMEDIATE:
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "parameter",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case STRING:
                function.addParameter(new Variable(
                    Variable.Type.STRING,
                    "parameter"
                ));
                break;
            
            case NONE:
                // Nothing
                break;
        }
        
        function.setInterpretable(new Print(type, operand));
        return function;
    }
}
