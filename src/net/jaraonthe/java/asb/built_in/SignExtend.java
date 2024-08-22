package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * The {@code &sign_extend} built-in function.<br>
 * 
 * {@code &sign_extend dstRegister, srcRegister}
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class SignExtend implements Interpretable
{
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue src = context.frame.getNumericValue("src");
        NumericValue dst = context.frame.getNumericValue("dst");
        if (src.length > dst.length) {
            throw new RuntimeError(
                "Cannot &sign_extend from bigger variable " + src.getReferencedName()
                + " to smaller variable " + dst.getReferencedName()
            );
        }
        
        BigInteger value = src.read(context);
        if (src.length == dst.length || !value.testBit(src.length - 1)) {
            dst.write(value, context);
            return;
        }
        
        /* The actual sign extend */

        // Amount of src/dst bytes that have actual content
        int srcContentBytes = Math.ceilDiv(src.length, 8);
        int dstContentBytes = Math.ceilDiv(dst.length, 8);
        // Amount of dst bytes incl. leading 0 byte to enforce positive value
        int dstTotalBytes = dst.length / 8 + 1;
        
        // Amount of content bits in the src/dst most significant content byte
        // 0 means 8 bits
        int srcLeadingByteBits = src.length % 8;
        int dstLeadingByteBits = dst.length % 8;
        
        // Byte arrays: MSB is in element #0 (big-endian)
        byte[] content = value.toByteArray(); // Minimum length two's-complement
        byte[] extended = new byte[dstTotalBytes];
        System.arraycopy(content, 0, extended, extended.length - content.length, content.length);
        
        // Fill up the MSB byte of the source
        if (srcLeadingByteBits != 0) {
            int fillBits = (0xFF << (srcLeadingByteBits)) & 0xFF;
            
            if (srcContentBytes == dstContentBytes && dstLeadingByteBits != 0) {
                // This is also the MSB byte for the destination
                fillBits = fillBits & (0xFF >> (8 - dstLeadingByteBits));
            }
            extended[extended.length - srcContentBytes] |= fillBits;
        }

        // Leading high bit bytes
        for (int i = extended.length - srcContentBytes - 1; i >= 1; i--) {
            extended[i] = (byte)-1;
        }
        
        // Fill up the MSB byte of the destination
        if (dstContentBytes == dstTotalBytes && srcContentBytes != dstContentBytes) {
            extended[0] = (byte)(0xFF >> (8 - dstLeadingByteBits));
        }
        
        dst.write(new BigInteger(extended), context);
    }
    
    
    /**
     * Creates a {@code &sign_extend} built-in function.
     * 
     * @return
     */
    public static BuiltInFunction create()
    {
        BuiltInFunction function = new BuiltInFunction("&sign_extend", false);
        
        // &sign_extend dstRegister, srcRegister
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
        
        function.setInterpretable(new SignExtend());
        return function;
    }
}
