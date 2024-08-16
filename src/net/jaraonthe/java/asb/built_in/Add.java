package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Interpretable;
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
        ADD("&add"),
        ADDC("&addc"),
        SUB("&sub"),
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
        this.type = type;
        this.operands = operands;
    }
    
    // TODO interpret() once designed
    
    
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
        
        function.setInterpretable(new Add(type, operands));
        return function;
    }
}
