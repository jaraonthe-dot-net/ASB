package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &add} and {@code &addc} built-in functions.<br>
 * 
 * {@code &add dstRegister, src1Register, src2Imm};<br>
 * {@code &add dstRegister, src1Register, srcRegister2};<br>
 * {@code &addc dstRegister, src1Register, src2Imm};<br>
 * {@code &addc dstRegister, src1Register, srcRegister2};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Add implements Interpretable
{
    public enum Variant
    {
        // destination_source1_source2
        REG_REG_IMM, // &add dstRegister, src1Register, src2Imm
        REG_REG_REG, // &add dstRegister, src1Register, srcRegister2
    }
    
    private final Add.Variant variant;
    
    private final boolean isAddc;
    
    /**
     * @param variant Selects the operand variant of this command
     * @param isAddc  Selects the Add or Addc variant of this command
     */
    private Add(Add.Variant variant, boolean isAddc)
    {
        this.variant = variant;
        this.isAddc  = isAddc;
    }
    
    // TODO interpret() once designed
    
    
    /**
     * Creates a {@code &add} or {@code &addc} built-in function with the given variant.
     * 
     * @param variant Selects the operand variant of this command
     * @param isAddc  Selects the Add or Addc variant of this command
     * 
     * @return
     */
    public static BuiltInFunction create(Add.Variant variant, boolean isAddc)
    {
        BuiltInFunction function = new BuiltInFunction((isAddc ? "&addc" : "&add"), false);

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
        
        switch (variant) {
            case REG_REG_IMM:
                // &add dstRegister, src1Register, src2Imm
                function.addParameter(new Variable(
                    Variable.Type.IMMEDIATE,
                    "src2Imm",
                    Constraints.MAX_LENGTH
                ));
                break;
                
            case REG_REG_REG:
                // &add dstRegister, src1Register, srcRegister2
                function.addParameter(new Variable(
                    Variable.Type.REGISTER,
                    "src2Register",
                    Constraints.MIN_LENGTH,
                    Constraints.MAX_LENGTH
                ));
                break;
        }
        
        function.setInterpretable(new Add(variant, isAddc));
        return function;
    }
}
