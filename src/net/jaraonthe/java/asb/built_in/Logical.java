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
 * The {@code &and}, {@code &or}, and {@code &xor} built-in functions.<br>
 * 
 * {@code &and dstRegister, src1Register, src2Imm};<br>
 * {@code &and dstRegister, src1Register, srcRegister2};<br>
 * {@code &or  dstRegister, src1Register, src2Imm};<br>
 * {@code &or  dstRegister, src1Register, srcRegister2};<br>
 * {@code &xor dstRegister, src1Register, src2Imm};<br>
 * {@code &xor dstRegister, src1Register, srcRegister2};
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
    public enum Operands
    {
        // destination_source1_source2
        REG_REG_IMM, // &and dstRegister, src1Register, src2Imm
        REG_REG_REG, // &and dstRegister, src1Register, srcRegister2
    }
    
    private final Logical.Type type;
    private final Logical.Operands operands;
    
    /**
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     */
    private Logical(Logical.Type type, Logical.Operands operands)
    {
        this.type     = type;
        this.operands = operands;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src1 = context.frame.getNumericValue("src1");
        NumericValue src2 = context.frame.getNumericValue("src2");
        NumericValue dst  = context.frame.getNumericValue("dst");

        BigInteger src2Value = src2.read(context);
        // Check lengths
        if (
            src1.length != dst.length
            || (
                (this.operands == Logical.Operands.REG_REG_IMM) ?
                    (NumericValue.bitLength(src2Value) > dst.length)
                    : (src2.length != dst.length)
            )
        ) {
            throw new RuntimeError(
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
    
    
    /**
     * Creates a {@code &and}, {@code &or}, or {@code &xor} built-in function
     * with the given operands variant.
     * 
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     * 
     * @return
     */
    public static BuiltInFunction create(Logical.Type type, Logical.Operands operands)
    {
        BuiltInFunction function = new BuiltInFunction(type.functionName, false);

        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "dst",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        function.addCommandSymbols(",");
        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "src1",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        function.addCommandSymbols(",");
        
        switch (operands) {
            case REG_REG_IMM:
                // &and dstRegister, src1Register, src2Imm
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src2",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_REG_REG:
                // &and dstRegister, src1Register, srcRegister2
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src2",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Logical(type, operands));
        return function;
    }
}
