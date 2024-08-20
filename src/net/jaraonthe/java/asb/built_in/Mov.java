package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &mov} built-in function.<br>
 * 
 * {@code &mov @dstAddress, imm}<br>
 * {@code &mov @dstAddress, @srcAddress}<br>
 * {@code &mov @dstAddress, srcRegister}<br>
 * {@code &mov dstRegister, imm}<br>
 * {@code &mov dstRegister, @srcAddress}<br>
 * {@code &mov dstRegister, srcRegister}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Mov implements Interpretable
{
    public enum Operands
    {
        // destination_source
        MEM_IMM, // &mov @dstAddress, imm
        MEM_MEM, // &mov @dstAddress, @srcAddress
        MEM_REG, // &mov @dstAddress, srcRegister
        REG_IMM, // &mov dstRegister, imm
        REG_MEM, // &mov dstRegister, @srcAddress
        REG_REG, // &mov dstRegister, srcRegister
    }
    
    private final Mov.Operands operands;
    
    /**
     * @param operands Selects the operands of this command
     */
    private Mov(Mov.Operands operands)
    {
        this.operands = operands;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src;
        NumericValue dst;
        switch (this.operands) {
            case MEM_IMM:
            case MEM_MEM:
            case MEM_REG:
            case REG_MEM:
                // TODO memory (incl. lengths check)
                throw new RuntimeException("memory not implemented yet");
                
            case REG_IMM:
                src = context.frame.getNumericValue("src");
                dst = context.frame.getNumericValue("dst");
                BigInteger srcImm = src.read(context);
                if (srcImm.bitLength() + (srcImm.signum() < 0 ? 1 : 0) > dst.length) {
                    throw new RuntimeError(
                        "Cannot &mov immediate " + srcImm + " to variable "
                        + dst.getReferenced().variable.name + " as it is too big"
                    );
                }
                dst.write(srcImm, context);
                break;
                
            case REG_REG:
                src = context.frame.getNumericValue("src");
                dst = context.frame.getNumericValue("dst");
                if (src.length != dst.length) {
                    throw new RuntimeError(
                        "Cannot &mov between two variables " + src.getReferenced().variable.name
                        + " and " + dst.getReferenced().variable.name + " that do not have the same length"
                    );
                }
                dst.write(src.read(context), context);
                break;
        }
    }
    
    
    /**
     * Creates a {@code &mov} built-in function with the given operands.
     * 
     * @param operands
     * @return
     */
    public static BuiltInFunction create(Mov.Operands operands)
    {
        BuiltInFunction function = new BuiltInFunction("&mov", false);
        
        switch (operands) {
            case MEM_IMM:
                // &mov @dstAddress, imm
                function.addCommandSymbols("@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dstAddress",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case MEM_MEM:
                // &mov @dstAddress, @srcAddress
                function.addCommandSymbols("@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dstAddress",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "srcAddress",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case MEM_REG:
                // &mov @dstAddress, srcRegister
                function.addCommandSymbols("@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dstAddress",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_IMM:
                // &mov dstRegister, imm
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dst",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_MEM:
                // &mov dstRegister, @srcAddress
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dst",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "srcAddress",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_REG:
                // &mov dstRegister, srcRegister
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dst",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Mov(operands));
        return function;
    }
}
