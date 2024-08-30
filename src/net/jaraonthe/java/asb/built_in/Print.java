package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;

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
    
    public enum OperandType
    {
        IMMEDIATE (Variable.Type.IMMEDIATE),
        REGISTER  (Variable.Type.REGISTER),
        STRING    (Variable.Type.STRING),
        NONE      (null);
        
        public final Variable.Type variableType;
        
        private OperandType(Variable.Type type)
        {
            this.variableType = type;
        }
    }
    
    protected final Print.Type type;
    protected final Print.OperandType operand;
    
    
    /**
     * @param type    Selects the actual function
     * @param operand Selects the function variant (via the Operand type)
     */
    private Print(Print.Type type, Print.OperandType operand)
    {
        this.type    = type;
        this.operand = operand;
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
    public static BuiltInFunction create(Print.Type type, Print.OperandType operand)
    {
        if (operand == Print.OperandType.NONE && type != Print.Type.PRINTLN) {
            throw new IllegalArgumentException(
                "Cannot create &print function without operand"
            );
        }
        
        BuiltInFunction function = new BuiltInFunction(type.functionName, true);

        if (operand != Print.OperandType.NONE) {
            function.addParameterByType(operand.variableType, "parameter");
        }
        
        function.setInterpretable(new Print(type, operand));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        context.settings.printOccurred = true;
        switch (this.operand) {
            case REGISTER:
            case IMMEDIATE:
                // Always trigger a read on virtual registers and bitwise access
                System.out.print(BuiltInFunction.getNumericValue("parameter", context.frame).read(context));
                break;
            case STRING:
                String text = BuiltInFunction.getValue("parameter", context.frame).toString();
                System.out.print(text);
                if (text.charAt(text.length() - 1) == '\n') {
                    context.settings.printOccurred = false;
                }
                break;
            case NONE:
                // nothing
                break;
        }
        if (this.type == Print.Type.PRINTLN) {
            System.out.println();
            context.settings.printOccurred = false;
        }
    }
}
