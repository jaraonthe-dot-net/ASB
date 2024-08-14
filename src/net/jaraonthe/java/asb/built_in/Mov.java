package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Interpretable;
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
    public enum Variant
    {
        // destination_source
        MEM_IMM, // &mov @dstAddress, imm
        MEM_MEM, // &mov @dstAddress, @srcAddress
        MEM_REG, // &mov @dstAddress, srcRegister
        REG_IMM, // &mov dstRegister, imm
        REG_MEM, // &mov dstRegister, @srcAddress
        REG_REG, // &mov dstRegister, srcRegister
    }
    
    private final Mov.Variant variant;
    
    /**
     * @param variant Selects the variant of this command
     */
    private Mov(Mov.Variant variant)
    {
        this.variant = variant;
    }
    
    // TODO interpret() once designed
    
    
    /**
     * Creates a {@code &mov} built-in function with the given variant.
     * 
     * @param variant
     * @return
     */
    public static BuiltInFunction create(Mov.Variant variant)
    {
        BuiltInFunction function = new BuiltInFunction("&mov", false);
        
        switch (variant) {
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
                    "imm",
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
                    "srcRegister",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_IMM:
                // &mov dstRegister, imm
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dstRegister",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "imm",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_MEM:
                // &mov dstRegister, @srcAddress
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "dstRegister",
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
                    "dstRegister",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                function.addCommandSymbols(",");
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "srcRegister",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Mov(variant));
        return function;
    }
}
