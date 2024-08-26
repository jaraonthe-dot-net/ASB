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
        MEM_IMM(true), // &mov @dstAddress, imm
        MEM_MEM(true), // &mov @dstAddress, @srcAddress
        MEM_REG(true), // &mov @dstAddress, srcRegister
        REG_IMM(false), // &mov dstRegister, imm
        REG_MEM(true), // &mov dstRegister, @srcAddress
        REG_REG(false); // &mov dstRegister, srcRegister
        
        public final boolean usesMemory;
        
        private Operands(boolean usesMemory)
        {
            this.usesMemory = usesMemory;
        }
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
        if (this.operands.usesMemory && context.memory == null) {
            throw new RuntimeError("Cannot &mov to/from memory as it is not configured");
        }
        
        NumericValue src = context.frame.getNumericValue("src");
        NumericValue dst = context.frame.getNumericValue("dst");
        switch (this.operands) {
            case MEM_IMM:
            case MEM_REG:
                context.memory.write(
                    dst.read(context), // @dstAddress
                    src.read(context)  // imm/srcRegister
                );
                break;
                
            case MEM_MEM:
                context.memory.write(
                    dst.read(context),    // @dstAddress
                    context.memory.read(
                        src.read(context) // @srcAddress
                    )
                );
                break;
                
            case REG_MEM:
                if (dst.length != context.memory.wordLength) {
                    throw new RuntimeError(
                        "Cannot &mov memory word to variable " + dst.getReferencedName()
                        + " as it has a different length"
                    );
                }
                
                dst.write(
                    context.memory.read(
                        src.read(context) // @srcAddress
                    ),
                    context
                );
                break;
                
            case REG_IMM:
                BigInteger srcImm = src.read(context);
                if (NumericValue.bitLength(srcImm) > dst.length) {
                    throw new RuntimeError(
                        "Cannot &mov immediate " + srcImm + " to variable "
                        + dst.getReferencedName() + " as it is too big"
                    );
                }
                dst.write(srcImm, context);
                break;
                
            case REG_REG:
                if (src.length != dst.length) {
                    throw new RuntimeError(
                        "Cannot &mov between two variables " + src.getReferencedName()
                        + " and " + dst.getReferencedName() + " that do not have the same length"
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
                
            case MEM_MEM:
                // &mov @dstAddress, @srcAddress
                function.addCommandSymbols("@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dst",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",@");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case MEM_REG:
                // &mov @dstAddress, srcRegister
                function.addCommandSymbols("@");
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
                    "src",
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
