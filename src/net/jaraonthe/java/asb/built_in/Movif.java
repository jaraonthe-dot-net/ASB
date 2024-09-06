package net.jaraonthe.java.asb.built_in;


import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * The {@code &movif} built-in function.<br>
 * 
 * {@code &movif a == b, dst, src};<br>
 * {@code &movif a >  b, dst, src};<br>
 * {@code &movif a >= b, dst, src};<br>
 * {@code &movif a <  b, dst, src};<br>
 * {@code &movif a <= b, dst, src};<br>
 * {@code &movif a != b, dst, src};<br>
 * <br>
 * {@code a} and {@code b} are (individually) either /register or /immediate
 * (but at least one must be /register).<br>
 * {@code dst} may be {@code @dstAddress} or {@code dstRegister}. {@code src}
 * may be {@code imm}, {@code @srcAddress}, or {@code srcRegister}. 
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Movif extends Compare
{
    protected final Mov.OperandType dst;
    protected final Mov.OperandType src;
    
    /**
     * @param operator
     * @param a
     * @param b
     * @param dst Must not be IMMEDIATE
     * @param src
     */
    private Movif(
        Compare.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b,
        Mov.OperandType dst,
        Mov.OperandType src
    ) {
        super(operator, a, b);
        this.dst = dst;
        this.src = src;
    }

    /**
     * Creates a {@code &movif} built-in function as configured.<br>
     * 
     * At least one of a or b must be REG.
     * 
     * @param operator
     * @param a
     * @param b
     * @param dst Must not be IMMEDIATE
     * @param src
     * 
     * @return
     */
    public static BuiltInFunction create(
        Compare.Operator operator,
        BuiltInFunction.OperandType a,
        BuiltInFunction.OperandType b,
        Mov.OperandType dst,
        Mov.OperandType src
    ) {
        BuiltInFunction function = new BuiltInFunction("&movif", false);
        
        Compare.addComparisonOperands(function, operator, a, b);
        function.addCommandSymbols(",");
        Mov.addOperands(function, dst, src);
        
        function.setInterpretable(new Movif(operator, a, b, dst, src));
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        if (this.compare(context)) {
            Mov.move(context, this.dst, this.src);
        }
    }
}
