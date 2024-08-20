package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &add}, {@code &addc}, {@code &sub}, and {@code &subc} built-in
 * functions.<br>
 * 
 * {@code &add dstRegister, src1Register, src2Imm};<br>
 * {@code &add dstRegister, src1Register, srcRegister2};<br>
 * {@code &addc dstRegister, src1Register, src2Imm};<br>
 * {@code &addc dstRegister, src1Register, srcRegister2};<br>
 * {@code &sub dstRegister, src1Register, src2Imm};<br>
 * {@code &sub dstRegister, src1Register, srcRegister2};<br>
 * {@code &subc dstRegister, src1Register, src2Imm};<br>
 * {@code &subc dstRegister, src1Register, srcRegister2};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Add implements Interpretable
{
    public enum Type
    {
        ADD ("&add"),
        ADDC("&addc"),
        SUB ("&sub"),
        SUBC("&subc");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    public enum Operands
    {
        // destination_source1_source2
        REG_REG_IMM, // &add dstRegister, src1Register, src2Imm
        REG_REG_REG, // &add dstRegister, src1Register, srcRegister2
    }
    
    private final Add.Type type;
    private final Add.Operands operands;
    
    /**
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     */
    private Add(Add.Type type, Add.Operands operands)
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
        
        // Check lengths
        switch (this.type) {
            case ADD:
            case SUB:
                if (src1.length != dst.length || src2.length != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " two variables "
                        + src1.getReferenced().variable.name + " and " + src2.getReferenced().variable.name
                        + " that do not have the same length as the destination variable "
                        + dst.getReferenced().variable.name
                    );
                }
                break;
                
            case ADDC:
            case SUBC:
                if (src1.length != src2.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " two variables "
                        + src1.getReferenced().variable.name + " and " + src2.getReferenced().variable.name
                        + " that do not have the same length"
                    );
                }
                if (src1.length + 1 != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " into destination variable "
                        + dst.getReferenced().variable.name
                        + " as it does not have the expected length (src length + 1)"
                    );
                }
                break;
        }
        
        BigInteger src1Value = src1.read(context);
        BigInteger src2Value = src2.read(context);
        BigInteger result = null;
        switch (this.type) {
            case ADD:
            case ADDC:
                result = src1Value.add(src2Value);
                break;
            case SUB:
            case SUBC:
                result = src1Value.subtract(src2Value);
                break;
        }
        if (this.type == Add.Type.ADD || this.type == Add.Type.SUB) {
            // Cut off potential carry-out
            result = result.clearBit(dst.length);
        }
        
        dst.write(result, context);
    }
    
    
    /**
     * Creates a {@code &add}, {@code &addc}, {@code &sub}, or {@code &subc}
     * built-in function with the given operands variant.
     * 
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     * 
     * @return
     */
    public static BuiltInFunction create(Add.Type type, Add.Operands operands)
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
                // &add dstRegister, src1Register, src2Imm
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src2",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_REG_REG:
                // &add dstRegister, src1Register, srcRegister2
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src2",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Add(type, operands));
        return function;
    }
}
