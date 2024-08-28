package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &get_memory_word_length}, {@code &get_memory_address_length} (aka
 * {@code &get_memory_addr_length}), and {@code &get_program_counter_length}
 * (aka {@code &get_pc_length}) built-in functions.<br>
 * 
 * {@code &get_memory_word_length     dstLength};<br>
 * {@code &get_memory_address_length  dstLength};<br>
 * {@code &get_memory_addr_length     dstLength};<br>
 * {@code &get_program_counter_length dstLength};<br>
 * {@code &get_pc_length              dstLength};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class SystemInfo implements Interpretable
{
    public enum Type
    {
        GET_MEMORY_WORD_LENGTH    ("&get_memory_word_length"),
        GET_MEMORY_ADDRESS_LENGTH ("&get_memory_address_length"),
        GET_MEMORY_ADDR_LENGTH    ("&get_memory_addr_length"),
        GET_PROGRAM_COUNTER_LENGTH("&get_program_counter_length"),
        GET_PC_LENGTH             ("&get_pc_length");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    
    protected final SystemInfo.Type type;
    
    
    /**
     * @param type Selects the actual function
     */
    private SystemInfo(SystemInfo.Type type)
    {
        this.type = type;
    }
    
    /**
     * Creates a {@code &get_memory_word_length},
     * {@code &get_memory_address_length}, {@code &get_memory_addr_length},
     * {@code &get_program_counter_length}, or {@code &get_pc_length} built-in
     * function.
     * 
     * @param type Selects the actual function
     * 
     * @return
     */
    public static BuiltInFunction create(SystemInfo.Type type)
    {
        BuiltInFunction function = new BuiltInFunction(type.functionName, false);

        function.addParameterByType(Variable.Type.REGISTER, "dst");
        
        function.setInterpretable(new SystemInfo(type));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        BigInteger length = BigInteger.valueOf(
            switch (this.type) {
                case GET_MEMORY_WORD_LENGTH -> context.ast.getMemoryWordLength();
                case GET_MEMORY_ADDRESS_LENGTH,
                    GET_MEMORY_ADDR_LENGTH  -> context.ast.getMemoryAddressLength();
                case GET_PROGRAM_COUNTER_LENGTH,
                    GET_PC_LENGTH           -> context.ast.getPcLength();
            }
        );
        
        NumericValue dst = context.frame.getNumericValue("dst");
        if (length.bitLength() > dst.length) {
            throw new RuntimeError(
                "Cannot store result of " + this.type.functionName + " in "
                + dst.getReferencedName() + " as the result value is too big"
            );
        }
        
        dst.write(length, context);
    }
}
