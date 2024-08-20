package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.parse.Constraints;

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
public class Assert implements Interpretable
{
    public enum OperandType
    {
        IMM,
        REG
    }
    
    public enum Operator
    {
        EQUALS                ("=="),
        GREATER_THAN          (">"),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN             ("<"),
        LESS_THAN_OR_EQUALS   ("<="),
        NOT_EQUALS            ("!=");
        
        public final String symbols;
        
        private Operator(String symbols)
        {
            this.symbols = symbols;
        }
    }
    
    public final Assert.Operator operator;
    public final Assert.OperandType a;
    public final Assert.OperandType b;
    public final boolean hasMessage;
    
    
    /**
     * At least one of a or b must be REG.
     * 
     * @param operator
     * @param a
     * @param b
     * @param hasMessage
     */
    private Assert(
        Assert.Operator operator,
        Assert.OperandType a,
        Assert.OperandType b,
        boolean hasMessage
    ) {
        this.operator = operator;
        this.a        = a;
        this.b        = b;
        this.hasMessage = hasMessage;
    }

    
    @Override
    public void interpret(Context context)
    {
        // TODO
        throw new RuntimeException("&assert not implemented yet");
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
        Assert.OperandType a,
        Assert.OperandType b,
        boolean hasMessage
    ) {
        if (a == Assert.OperandType.IMM && b == Assert.OperandType.IMM) {
            throw new IllegalArgumentException(
                "Cannot create &assert function with two immediate operands"
            );
        }
        
        BuiltInFunction function = new BuiltInFunction("&assert", false);
        
        Assert.addOperand(function, a, "a");
        function.addCommandSymbols(operator.symbols);
        Assert.addOperand(function, b, "b");
        
        if (hasMessage) {
            function.addCommandSymbols(",");
            function.addParameter(new Variable(Variable.Type.STRING, "message"));
        }
        
        function.setInterpretable(new Assert(operator, a, b, hasMessage));
        return function;
    }
    
    /**
     * @param function
     * @param type
     * @param name
     */
    private static void addOperand(BuiltInFunction function, Assert.OperandType type, String name)
    {
        switch (type) {
        case IMM:
            function.addParameter(new Variable(
                Variable.Type.IMMEDIATE,
                name,
                Constraints.MAX_LENGTH
            ));
            break;
        case REG:
            function.addParameter(new Variable(
                Variable.Type.REGISTER,
                name,
                Constraints.MIN_LENGTH,
                Constraints.MAX_LENGTH
            ));
            break;
        }
    }
}
