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
 * The {@code &add}, {@code &addc}, {@code &sub}, and {@code &subc} built-in
 * functions.<br>
 * 
 * {@code &add  dstRegister, src1Register, src2Imm};<br>
 * {@code &add  dstRegister, src1Register, srcRegister2};<br>
 * {@code &addc dstRegister, src1Register, src2Imm};<br>
 * {@code &addc dstRegister, src1Register, srcRegister2};<br>
 * <br>
 * {@code &sub  dstRegister, src1Register, src2Imm};<br>
 * {@code &sub  dstRegister, src1Register, srcRegister2};<br>
 * {@code &sub  dstRegister, src1Imm,      src2Register};<br>
 * {@code &subc dstRegister, src1Register, src2Imm};<br>
 * {@code &subc dstRegister, src1Register, srcRegister2};<br>
 * {@code &subc dstRegister, src1Imm,      src2Register};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
// TODO Rename to Arithmetic and add multiplication, division, remainder
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
        REG_REG_REG, // &add dstRegister, src1Register, src2Register
        
        REG_IMM_REG, // &sub dstRegister, src1Imm, src2Register // only for &sub/&subc
    }
    
    protected final Add.Type type;
    protected final Add.Operands operands;
    
    
    /**
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     */
    private Add(Add.Type type, Add.Operands operands)
    {
        this.type     = type;
        this.operands = operands;
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
        
        if (!Add.isValidCombination(type, operands)) {
            throw new IllegalArgumentException(
                "Cannot create " + type.functionName +  " function with /reg, /imm, /reg operands"
            );
        }

        function.addParameter(new Variable(
            Variable.Type.REGISTER,
            "dst",
            Constraints.MIN_LENGTH,
            Constraints.MAX_LENGTH
        ));
        
        function.addCommandSymbols(",");
        switch (operands) {
            case REG_REG_IMM:
            case REG_REG_REG:
                // &add dstRegister, src1Register, src2Imm
                // &add dstRegister, src1Register, src2Register
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src1",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_IMM_REG:
                // &sub dstRegister, src1Imm, src2Register
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src1",
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
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
            case REG_IMM_REG:
                // &add dstRegister, src1Register, src2Register
                // &sub dstRegister, src1Imm, src2Register
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
    
    /**
     * @param type
     * @param operands
     * 
     * @return True if the given values are a valid combination, i.e.
     *         {@link #create()} will create a function with this input.
     */
    public static boolean isValidCombination(Add.Type type, Add.Operands operands)
    {
        return operands != Add.Operands.REG_IMM_REG
            || type == Add.Type.SUB || type == Add.Type.SUBC;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src1 = context.frame.getNumericValue("src1");
        NumericValue src2 = context.frame.getNumericValue("src2");
        NumericValue dst  = context.frame.getNumericValue("dst");

        BigInteger src1Value = src1.read(context);
        BigInteger src2Value = src2.read(context);

        // Check lengths
        NumericValue srcReg    = src1;
        NumericValue srcImm    = src2; // which of course may also be a register
        BigInteger srcImmValue = src2Value;
        if (this.operands == Add.Operands.REG_IMM_REG) {
            srcReg      = src2;
            srcImm      = src1;
            srcImmValue = src1Value;
        }
        switch (this.type) {
            case ADD:
            case SUB:
                if (
                    srcReg.length != dst.length
                    || (
                        (this.operands != Add.Operands.REG_REG_REG) ?
                            (NumericValue.bitLength(srcImmValue) > dst.length)
                            : (srcImm.length != dst.length)
                    )
                ) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " two variables "
                        + src1.getReferencedName() + " and " + src2.getReferencedName()
                        + " that do not have the same length as the destination variable "
                        + dst.getReferencedName()
                    );
                }
                break;
                
            case ADDC:
            case SUBC:
                if (
                    this.operands != Add.Operands.REG_REG_REG ?
                        NumericValue.bitLength(srcImmValue) > srcReg.length
                        : srcImm.length != src1.length
                ) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " two variables "
                        + src1.getReferencedName() + " and " + src2.getReferencedName()
                        + " that do not have the same length"
                    );
                }
                if (srcReg.length + 1 != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " into destination variable "
                        + dst.getReferencedName()
                        + " as it does not have the expected length (src length + 1)"
                    );
                }
                break;
        }
        
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
        result = NumericValueStore.normalizeBigInteger(result, srcReg.length + 1);
        if (this.type == Add.Type.ADD || this.type == Add.Type.SUB) {
            // Cut off potential carry-out
            result = result.clearBit(dst.length);
        }
        
        dst.write(result, context);
    }
}
