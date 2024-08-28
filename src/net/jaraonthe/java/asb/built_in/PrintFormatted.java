package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &print_*} and {@code &println_*} built-in functions.
 * 
 * These functions can be invoked in userland as well.<br>
 * 
 * {@code &print_s register};<br>
 * {@code &print_x register};<br>
 * {@code &print_b register};<br>
 * {@code &println_s register};<br>
 * {@code &println_x register};<br>
 * {@code &println_b register};<br>
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
    
    protected final PrintFormatted.Type type;
    protected final PrintFormatted.Format format;
    
    
    /**
     * @param type   Selects the actual function
     * @param format Selects the function variant
     */
    private PrintFormatted(PrintFormatted.Type type, PrintFormatted.Format format)
    {
        this.type   = type;
        this.format = format;
    }

    /**
     * Creates a {@code &print_*} or {@code &println_*} built-in function with
     * the given format variant.
     * 
     * @param type   Selects the actual function
     * @param format Selects the function variant
     * 
     * @return
     */
    public static BuiltInFunction create(PrintFormatted.Type type, PrintFormatted.Format format)
    {
        BuiltInFunction function = new BuiltInFunction(
            type.functionNameMain + format.functionNamePostfix,
            true
        );

        function.addParameterByType(Variable.Type.REGISTER, "parameter");
        
        function.setInterpretable(new PrintFormatted(type, format));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue parameter = context.frame.getNumericValue("parameter");
        BigInteger value = parameter.read(context);
        switch (this.format) {
            case SIGNED:
                if (!value.testBit(parameter.length - 1)) {
                    System.out.print(value);
                    break;
                }
                System.out.print(this.twosComplement2Negative(value));
                break;
                
            case HEX:
                System.out.print("0x" + this.padToLength(
                    value.toString(16),
                    Math.ceilDiv(parameter.length, 4)
                ));
                break;
                
            case BINARY:
                System.out.print("0b" + this.padToLength(value.toString(2), parameter.length));
        }
        
        if (this.type == PrintFormatted.Type.PRINTLN) {
            System.out.println();
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
