package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

/**
 * The {@code &add}, {@code &addc}, {@code &sub}, and {@code &subc} built-in
 * functions.<br>
 * 
 * {@code &add  dstRegister, src1Register, src2Imm};<br>
 * {@code &add  dstRegister, src1Register, src2Register};<br>
 * {@code &addc dstRegister, src1Register, src2Imm};<br>
 * {@code &addc dstRegister, src1Register, src2Register};<br>
 * <br>
 * {@code &sub  dstRegister, src1Register, src2Imm};<br>
 * {@code &sub  dstRegister, src1Register, src2Register};<br>
 * {@code &sub  dstRegister, src1Imm,      src2Register};<br>
 * {@code &subc dstRegister, src1Register, src2Imm};<br>
 * {@code &subc dstRegister, src1Register, src2Register};<br>
 * {@code &subc dstRegister, src1Imm,      src2Register};<br>
 * <br>
 * {@code &mul dstRegister, src1Register, src2Imm};<br>
 * {@code &mul dstRegister, src1Register, src2Imm};<br>
 * <br>
 * {@code &div dstRegister, src1Register, src2Imm};<br>
 * {@code &div dstRegister, src1Register, src2Register};<br>
 * {@code &div dstRegister, src1Imm,      src2Register};<br>
 * <br>
 * {@code &rem dstRegister, src1Register, src2Imm};<br>
 * {@code &rem dstRegister, src1Register, src2Register};<br>
 * {@code &rem dstRegister, src1Imm,      src2Register};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Arithmetic implements Interpretable
{
    public enum Type
    {
        ADD ("&add"),
        ADDC("&addc"),
        SUB ("&sub"),
        SUBC("&subc"),
        MUL ("&mul"),
        DIV ("&div"),
        REM ("&rem");
        
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
    
    protected final Arithmetic.Type type;
    protected final Arithmetic.Operands operands;
    
    
    /**
     * @param type     Selects the actual function
     * @param operands Selects the function variant (via the Operands set up)
     */
    private Arithmetic(Arithmetic.Type type, Arithmetic.Operands operands)
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
    public static BuiltInFunction create(Arithmetic.Type type, Arithmetic.Operands operands)
    {
        BuiltInFunction function = new BuiltInFunction(type.functionName, false);
        
        if (!Arithmetic.isValidCombination(type, operands)) {
            throw new IllegalArgumentException(
                "Cannot create " + type.functionName +  " function with /reg, /imm, /reg operands"
            );
        }

        function.addParameterByType(Variable.Type.REGISTER, "dst");
        
        function.addCommandSymbols(",");
        switch (operands) {
            case REG_REG_IMM:
            case REG_REG_REG:
                // &add dstRegister, src1Register, src2Imm
                // &add dstRegister, src1Register, src2Register
                function.addParameterByType(Variable.Type.REGISTER, "src1");
                break;
                
            case REG_IMM_REG:
                // &sub dstRegister, src1Imm, src2Register
                function.addParameterByType(Variable.Type.IMMEDIATE, "src1");
                break;
        }
        
        function.addCommandSymbols(",");
        switch (operands) {
            case REG_REG_IMM:
                // &add dstRegister, src1Register, src2Imm
                function.addParameterByType(Variable.Type.IMMEDIATE, "src2");
                break;
                
            case REG_REG_REG:
            case REG_IMM_REG:
                // &add dstRegister, src1Register, src2Register
                // &sub dstRegister, src1Imm, src2Register
                function.addParameterByType(Variable.Type.REGISTER, "src2");
                break;
        }
        
        function.setInterpretable(new Arithmetic(type, operands));
        return function;
    }
    
    /**
     * @param type
     * @param operands
     * 
     * @return True if the given values are a valid combination, i.e.
     *         {@link #create()} will create a function with this input.
     */
    public static boolean isValidCombination(Arithmetic.Type type, Arithmetic.Operands operands)
    {
        if (operands != Arithmetic.Operands.REG_IMM_REG) {
            return true;
        }
        
        switch (type) {
            case SUB:
            case SUBC:
            case DIV:
            case REM:
                return true;
            default:
                return false;
        }
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
        if (this.operands == Arithmetic.Operands.REG_IMM_REG) {
            srcReg      = src2;
            srcImm      = src1;
            srcImmValue = src1Value;
        }
        if (
            this.operands != Arithmetic.Operands.REG_REG_REG ?
                NumericValue.bitLength(srcImmValue) > srcReg.length
                : srcImm.length != srcReg.length
        ) {
            throw new RuntimeError(
                "Cannot " + this.type.functionName + " two variables "
                + src1.getReferencedName() + " and " + src2.getReferencedName()
                + " that do not have the same length"
            );
        }
        switch (this.type) {
            case ADD:
            case SUB:
            case DIV:
            case REM:
                if (srcReg.length != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " into destination variable "
                        + dst.getReferencedName()
                        + " as it does not have the same length as the source variables"
                    );
                }
                break;
                
            case ADDC:
            case SUBC:
                if (srcReg.length + 1 != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " into destination variable "
                        + dst.getReferencedName()
                        + " as it does not have the expected length (src length + 1)"
                    );
                }
                break;
                
            case MUL:
                if (srcReg.length * 2 != dst.length) {
                    throw new RuntimeError(
                        "Cannot " + this.type.functionName + " into destination variable "
                        + dst.getReferencedName()
                        + " as it does not have the expected length (src length * 2)"
                    );
                }
                break;
        }
        
        src1Value = NumericValueStore.normalizeBigInteger(src1Value, srcReg.length);
        src2Value = NumericValueStore.normalizeBigInteger(src2Value, srcReg.length);
        BigInteger dstValue = null;
        try {
            switch (this.type) {
                case ADD:
                case ADDC:
                    dstValue = src1Value.add(src2Value);
                    break;
                    
                case SUB:
                case SUBC:
                    dstValue = src1Value.subtract(src2Value);
                    break;
                    
                case MUL:
                    dstValue = src1Value.multiply(src2Value);
                    break;
                    
                case DIV:
                    dstValue = src1Value.divide(src2Value);
                    break;
                    
                case REM:
                    dstValue = src1Value.remainder(src2Value);
                    break;
            }
        } catch (ArithmeticException e) {
            throw new RuntimeError("Division by 0");
        }
        if (this.type == Arithmetic.Type.ADD || this.type == Arithmetic.Type.SUB) {
            // Cut off potential carry-out
            dstValue = dstValue.clearBit(dst.length);
        }
        
        dst.write(dstValue, context);
    }
}
