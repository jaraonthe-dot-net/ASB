package net.jaraonthe.java.asb.built_in;


import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * The {@code &jumpif} built-in function.<br>
 * 
 * {@code &jumpif a == b, label};<br>
 * {@code &jumpif a >  b, label};<br>
 * {@code &jumpif a >= b, label};<br>
 * {@code &jumpif a <  b, label};<br>
 * {@code &jumpif a <= b, label};<br>
 * {@code &jumpif a != b, label};<br>
 * {@code a} and {@code b} are (individually) either /register or /immediate
 * (but at least one must be /register).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Jumpif extends Compare
{
    /**
     * @param operator
     * @param a
     * @param b
     */
    private Jumpif(
        Jumpif.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b
    ) {
        super(operator, a, b);
    }

    /**
     * Creates a {@code &jumpif} built-in function as configured.<br>
     * 
     * At least one of a or b must be REG.
     * 
     * @param operator
     * @param a
     * @param b
     * 
     * @return
     */
    public static BuiltInFunction create(
        Jumpif.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b
    ) {
        BuiltInFunction function = new BuiltInFunction("&jumpif", false);
        
        Compare.addComparisonOperands(function, operator, a, b);
        
        function.addCommandSymbols(",");
        Jump.addLabelParameter(function);
        
        function.setInterpretable(new Jumpif(operator, a, b));
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        if (this.compare(context)) {
            Jump.jump(context);
        }
    }
}
