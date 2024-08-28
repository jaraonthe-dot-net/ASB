package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * Contains common code for built-in functions that compare two values with each
 * other (e.g. {@link Assert}, {@link Jumpif}).
 * 
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Compare implements Interpretable
{
    public enum OperandType
    {
        IMMEDIATE,
        REGISTER
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
    
    protected final Compare.Operator operator;
    protected final Compare.OperandType a;
    protected final Compare.OperandType b;
    
    
    /**
     * At least one of a or b must be REG.
     * 
     * @param operator
     * @param a
     * @param b
     */
    protected Compare(
        Compare.Operator operator,
        Compare.OperandType a,
        Compare.OperandType b
    ) {
        this.operator   = operator;
        this.a          = a;
        this.b          = b;
    }
    
    /**
     * Adds the comparison operands to the given function.<br>
     * 
     * At least one of a or b must be REG.
     * 
     * @param function
     * @param operator
     * @param a
     * @param b
     * 
     * @return
     */
    protected static void addComparisonOperands(
        BuiltInFunction function,
        Compare.Operator operator,
        Compare.OperandType a,
        Compare.OperandType b
    ) {
        if (a == Compare.OperandType.IMMEDIATE && b == Compare.OperandType.IMMEDIATE) {
            throw new IllegalArgumentException(
                "Cannot create " + function.name + " function with two immediate operands"
            );
        }
        
        Compare.addOperand(function, a, "a");
        function.addCommandSymbols(operator.symbols);
        Compare.addOperand(function, b, "b");
    }
    
    /**
     * @param function
     * @param type
     * @param name
     */
    private static void addOperand(BuiltInFunction function, Compare.OperandType type, String name)
    {
        switch (type) {
        case IMMEDIATE:
            function.addParameter(new Variable(
                Variable.Type.IMMEDIATE,
                name,
                Constraints.MAX_LENGTH
            ));
            break;
        case REGISTER:
            function.addParameter(new Variable(
                Variable.Type.REGISTER,
                name,
                Constraints.MIN_LENGTH,
                Constraints.MAX_LENGTH
            ));
            break;
        }
    }

    
    /**
     * Compares a and b parameters.
     * 
     * @param context
     * @return True if comparison according to {@link #operator} yields true
     * @throws RuntimeError
     */
    protected boolean compare(Context context) throws RuntimeError
    {
        return this.compare(
            context,
            context.frame.getNumericValue("a").read(context),
            context.frame.getNumericValue("b").read(context)
        );
        
    }
    
    /**
     * Compares a and b parameters.<br>
     * 
     * Here, the actual BigInteger values of a and b are passed in. This is
     * handy if the subclass wants to use these values as well, so they are not
     * read more than once.
     * 
     * @param context
     * @param aValue
     * @param bValue
     * 
     * @return True if comparison according to {@link #operator} yields true
     * 
     * @throws RuntimeError
     */
    protected boolean compare(Context context, BigInteger aValue, BigInteger bValue) throws RuntimeError
    {
        NumericValue a = context.frame.getNumericValue("a");
        NumericValue b = context.frame.getNumericValue("b");
        
        int cmp          = 0;
        boolean compared = false;
        // Normalize negative numbers
        if (aValue.signum() < 0) {
            try {
                aValue = NumericValueStore.normalizeBigInteger(
                    aValue,
                    // Immediates are normalized to length of other operand
                    this.a == Compare.OperandType.IMMEDIATE ? b.length : a.length
                );
            } catch (IllegalArgumentException e) {
                // a is greater than b (as it is too big for b.length)
                cmp      = 1;
                compared = true;
            }
        }
        if (bValue.signum() < 0) {
            try {
            bValue = NumericValueStore.normalizeBigInteger(
                bValue,
                // Ditto
                this.b == Compare.OperandType.IMMEDIATE ? a.length : b.length
            );
            } catch (IllegalArgumentException e) {
                // a is less than b (as b is too big for a.length)
                cmp      = -1;
                compared = true;
            }
        }
        if (!compared) {
            cmp = aValue.compareTo(bValue);
        }
        
        return switch (this.operator) {
            case EQUALS                 -> cmp == 0;
            case GREATER_THAN           -> cmp > 0;
            case GREATER_THAN_OR_EQUALS -> cmp >= 0;
            case LESS_THAN              -> cmp < 0;
            case LESS_THAN_OR_EQUALS    -> cmp <= 0;
            case NOT_EQUALS             -> cmp != 0;
        };
    }
}
