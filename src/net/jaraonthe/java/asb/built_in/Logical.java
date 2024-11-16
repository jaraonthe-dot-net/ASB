package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

/**
 * The {@code &and}, {@code &or}, and {@code &xor} built-in functions.<br>
 * 
 * {@code &and dstRegister, src1Register, src2Imm};<br>
 * {@code &and dstRegister, src1Register, src2Register};<br>
 * {@code &or  dstRegister, src1Register, src2Imm};<br>
 * {@code &or  dstRegister, src1Register, src2Register};<br>
 * {@code &xor dstRegister, src1Register, src2Imm};<br>
 * {@code &xor dstRegister, src1Register, src2Register};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Logical implements Interpretable
{
    public enum Type
    {
        AND("&and"),
        OR ("&or"),
        XOR("&xor");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    
    protected final Logical.Type type;
    protected final BuiltInFunction.OperandType src2Type;
    
    
    /**
     * @param type     Selects the actual function
     * @param src2Type Selects the function variant (via the src2 type)
     */
    private Logical(Logical.Type type, BuiltInFunction.OperandType src2Type)
    {
        this.type = type;
        this.src2Type = src2Type;
    }

    /**
     * Creates a {@code &and}, {@code &or}, or {@code &xor} built-in function
     * with the given operands variant.
     * 
     * @param type     Selects the actual function
     * @param src2Type Selects the function variant (via the src2 type)
     * 
     * @return
     */
    public static BuiltInFunction create(Logical.Type type, BuiltInFunction.OperandType src2Type)
    {
        BuiltInFunction function = new BuiltInFunction(type.functionName, false);

        function.addParameterByType(Parameter.Type.REGISTER, "dst");
        function.addCommandSymbols(",");
        function.addParameterByType(Parameter.Type.REGISTER, "src1");
        function.addCommandSymbols(",");
        function.addParameterByType(src2Type, "src2");
        
        function.setInterpretable(new Logical(type, src2Type));
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        NumericValue src1 = BuiltInFunction.getNumericValue("src1", context.frame);
        NumericValue src2 = BuiltInFunction.getNumericValue("src2", context.frame);
        NumericValue dst  = BuiltInFunction.getNumericValue("dst", context.frame);

        BigInteger src2Value = src2.read(context);
        // Check lengths
        if (
            src1.length != dst.length
            || (
                (this.src2Type == BuiltInFunction.OperandType.IMMEDIATE) ?
                    (NumericValue.bitLength(src2Value) > dst.length)
                    : (src2.length != dst.length)
            )
        ) {
            throw new ConstraintException(
                "Cannot " + this.type.functionName + " two variables "
                + src1.getReferencedName() + " and " + src2.getReferencedName()
                + " that do not have the same length as the destination variable "
                + dst.getReferencedName()
            );
        }
        
        src2Value = NumericValueStore.normalizeBigInteger(src2Value, dst.length);
        BigInteger src1Value = src1.read(context);
        BigInteger result = null;
        switch (this.type) {
            case AND:
                result = src1Value.and(src2Value);
                break;
            case OR:
                result = src1Value.or(src2Value);
                break;
            case XOR:
                result = src1Value.xor(src2Value);
                break;
        }
        
        dst.write(result, context);
    }
}
