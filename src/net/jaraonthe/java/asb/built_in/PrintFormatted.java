package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &print_*} and {@code &println_*} built-in functions.
 * 
 * These functions can be invoked in userland as well.<br>
 * <br>
 * {@code &print_s register};<br>
 * {@code &print_s @addressReg};<br>
 * {@code &print_s @addressImm};<br>
 * {@code &print_x register};<br>
 * {@code &print_x @addressReg};<br>
 * {@code &print_x @addressImm};<br>
 * {@code &print_b register};<br>
 * {@code &print_b @addressReg};<br>
 * {@code &print_b @addressImm};<br>
 * {@code &println_s register};<br>
 * {@code &println_s @addressReg};<br>
 * {@code &println_s @addressImm};<br>
 * {@code &println_x register};<br>
 * {@code &println_x @addressReg};<br>
 * {@code &println_x @addressImm};<br>
 * {@code &println_b register};<br>
 * {@code &println_b @addressReg};<br>
 * {@code &println_b @addressImm};<br>
 * <br>
 * {@code @addressReg} and {@code @addressImm} are used to print a memory cell
 * at the address given in a register or immediate.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class PrintFormatted implements Interpretable
{
    public enum Type
    {
        PRINT  ("&print_"),
        PRINTLN("&println_");
        
        public final String functionNameMain;
        
        private Type(String functionNameMain)
        {
            this.functionNameMain = functionNameMain;
        }
    }
    
    public enum Format
    {
        SIGNED("s"),
        HEX   ("x"),
        BINARY("b");
        
        public final String functionNamePostfix;
        
        private Format(String functionNamePostfix)
        {
            this.functionNamePostfix = functionNamePostfix;
        }
    }
    
    public enum OperandType
    {
        REGISTER,
        ADDRESS_IMMEDIATE,
        ADDRESS_REGISTER;
    }
    
    protected final PrintFormatted.Type type;
    protected final PrintFormatted.Format format;
    protected final PrintFormatted.OperandType operandType;
    
    
    /**
     * @param type        Selects the actual function
     * @param format      Selects the function variant
     * @param operandType
     */
    private PrintFormatted(
        PrintFormatted.Type type,
        PrintFormatted.Format format,
        PrintFormatted.OperandType operandType
    ) {
        this.type        = type;
        this.format      = format;
        this.operandType = operandType;
    }

    /**
     * Creates a {@code &print_*} or {@code &println_*} built-in function with
     * the given format variant.
     * 
     * @param type        Selects the actual function
     * @param format      Selects the function variant
     * @param operandType
     * 
     * @return
     */
    public static BuiltInFunction create(
        PrintFormatted.Type type,
        PrintFormatted.Format format,
        PrintFormatted.OperandType operandType
    ) {
        BuiltInFunction function = new BuiltInFunction(
            type.functionNameMain + format.functionNamePostfix,
            true
        );

        if (operandType != PrintFormatted.OperandType.REGISTER) {
            function.addCommandSymbols("@");
        }
        if (operandType == PrintFormatted.OperandType.ADDRESS_IMMEDIATE) {
            function.addParameterByType(Variable.Type.IMMEDIATE, "parameter");
        } else {
            function.addParameterByType(Variable.Type.REGISTER, "parameter");
        }
        
        function.setInterpretable(new PrintFormatted(type, format, operandType));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        context.settings.printOccurred = true;
        BigInteger value;
        int length;
        switch (this.operandType) {
            case REGISTER:
                NumericValue parameter = BuiltInFunction.getNumericValue("parameter", context.frame);
                value                  = parameter.read(context);
                length                 = parameter.length;
                
                break;
                
            case ADDRESS_IMMEDIATE:
                value = Print.getValueViaAddressImm(
                    context,
                    this.type.functionNameMain + this.format.functionNamePostfix
                );
                length = context.memory.wordLength;
                break;
                
            case ADDRESS_REGISTER:
                value = Print.getValueViaAddressReg(
                    context,
                    this.type.functionNameMain + this.format.functionNamePostfix
                );
                length = context.memory.wordLength;
                break;

            default:
                throw new RuntimeException("impossible");
        }
        
        switch (this.format) {
            case SIGNED:
                if (!value.testBit(length - 1)) {
                    System.out.print(value);
                    break;
                }
                System.out.print(this.twosComplement2Negative(value));
                break;
                
            case HEX:
                System.out.print("0x" + this.padToLength(
                    value.toString(16),
                    Math.ceilDiv(length, 4)
                ));
                break;
                
            case BINARY:
                System.out.print("0b" + this.padToLength(value.toString(2), length));
        }
        
        if (this.type == PrintFormatted.Type.PRINTLN) {
            System.out.println();
            context.settings.printOccurred = false;
        }
    }
    
    /**
     * @param negativeValue A positive BigInteger that is representing a
     *                      negative number in two's-complement (i.e. its MSB is
     *                      1).
     * @return A negative BigInteger from negativeValue
     */
    private BigInteger twosComplement2Negative(BigInteger negativeValue)
    {
        byte[] content = negativeValue.toByteArray();
        content[0] |= (0xFF << (negativeValue.bitLength() % 8)) & 0xFF;
        return new BigInteger(content);
    }
    
    /**
     * @param number
     * @param length
     * 
     * @return given number left-padded to given length with 0s
     */
    private String padToLength(String number, int length)
    {
        return String.format("%" + length + "s", number).replace(' ', '0');
    }
}
