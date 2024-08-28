package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.AssertError;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &assert} built-in function.<br>
 * 
 * {@code &assert a == b};<br>
 * {@code &assert a > b};<br>
 * {@code &assert a >= b};<br>
 * {@code &assert a < b};<br>
 * {@code &assert a <= b};<br>
 * {@code &assert a != b};<br>
 * {@code a} and {@code b} are (individually) either /register or /immediate
 * (but at least one must be /register).<br>
 * 
 * Additionally a /string error message can be given, like so:<br>
 * {@code &assert a == b, message};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Assert extends Compare
{
    protected final boolean hasMessage;

    
    /**
     * @param operator
     * @param a
     * @param b
     * @param hasMessage
     */
    private Assert(
        Assert.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b,
        boolean hasMessage
    ) {
        super(operator, a, b);
        this.hasMessage = hasMessage;
    }
    
    /**
     * Creates a {@code &assert} built-in function as configured.<br>
     * 
     * At least one of a or b must be REG.
     * 
     * @param operator
     * @param a
     * @param b
     * @param hasMessage
     * 
     * @return
     */
    public static BuiltInFunction create(
        Assert.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b,
        boolean hasMessage
    ) {
        BuiltInFunction function = new BuiltInFunction("&assert", true);
        
        Compare.addComparisonOperands(function, operator, a, b);
        
        if (hasMessage) {
            function.addCommandSymbols(",");
            function.addParameter(new Variable(Variable.Type.STRING, "message"));
        }
        
        function.setInterpretable(new Assert(operator, a, b, hasMessage));
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue a = context.frame.getNumericValue("a");
        NumericValue b = context.frame.getNumericValue("b");
        BigInteger aValue = a.read(context);
        BigInteger bValue = b.read(context);
        
        if (this.compare(context, aValue, bValue)) {
            return;
        }
        
        if (this.hasMessage) {
            throw new AssertError(context.frame.getValue("message").toString());
        }
        throw new AssertError(
            "Assert failed: " + a.getReferencedName() + " " + this.operator.symbols + " " + b.getReferencedName()
            + " (Values: " + aValue + " " + this.operator.symbols + " " + bValue + ")"
        );
    }
}
