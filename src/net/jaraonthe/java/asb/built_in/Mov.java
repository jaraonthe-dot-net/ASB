package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

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
    public enum OperandType
    {
        IMMEDIATE,
        REGISTER,
        ADDRESS;
    }
    
    protected final Mov.OperandType dst;
    protected final Mov.OperandType src;
    
    
    /**
     * @param dst Must not be IMMEDIATE
     * @param src
     */
    private Mov(Mov.OperandType dst, Mov.OperandType src)
    {
        this.dst = dst;
        this.src = src;
    }
    
    /**
     * Creates a {@code &mov} built-in function with the given operands.
     * 
     * @param dst Must not be IMMEDIATE
     * @param src
     * 
     * @return
     */
    public static BuiltInFunction create(Mov.OperandType dst, Mov.OperandType src)
    {
        BuiltInFunction function = new BuiltInFunction("&mov", false);
        
        Mov.addOperands(function, dst, src);
        
        function.setInterpretable(new Mov(dst, src));
        return function;
    }
    
    /**
     * Adds the {@code &mov} destination and source operands to the given function.
     * 
     * @param function
     * @param dst
     * @param src
     */
    public static void addOperands(BuiltInFunction function, Mov.OperandType dst, Mov.OperandType src)
    {
        if (dst == Mov.OperandType.IMMEDIATE) {
            throw new IllegalArgumentException(
                "Cannot create &mov function with an immediate destination operand"
            );
        }
        
        Mov.addOperand(function, dst, "dst");
        function.addCommandSymbols(",");
        Mov.addOperand(function, src, "src");
    }
    
    /**
     * @param function
     * @param type
     * @param name
     */
    private static void addOperand(BuiltInFunction function, Mov.OperandType type, String name)
    {
        switch (type) {
        case IMMEDIATE:
            function.addParameterByType(Variable.Type.IMMEDIATE, name);
            break;
        case ADDRESS:
            function.addCommandSymbols("@");
            // Fall-through
        case REGISTER:
            function.addParameterByType(Variable.Type.REGISTER, name);
            break;
        }
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        Mov.move(context, this.dst, this.src);
    }
    
    /**
     * Executes a move.
     * 
     * @param context
     * @param dst
     * @param src
     * 
     * @throws RuntimeError
     */
    public static void move(Context context, Mov.OperandType dst, Mov.OperandType src) throws RuntimeError
    {
        if (
            (dst == Mov.OperandType.ADDRESS || src == Mov.OperandType.ADDRESS)
            && context.memory == null
        ) {
            throw new RuntimeError("Cannot &mov to/from memory as it is not configured");
        }
        
        NumericValue srcValue = context.frame.getNumericValue("src");
        NumericValue dstValue = context.frame.getNumericValue("dst");
        
        if (dst == Mov.OperandType.ADDRESS) {
            Mov.checkAddress(dstValue, context, "to");
        }
        if (src == Mov.OperandType.ADDRESS) {
            Mov.checkAddress(srcValue, context, "from");
        }
        
        switch (src) {
            case IMMEDIATE:
                BigInteger srcImm = srcValue.read(context);
                switch (dst) {
                    case ADDRESS:
                        if (NumericValue.bitLength(srcImm) > context.memory.wordLength) {
                            throw new RuntimeError(
                                "Cannot &mov immediate " + srcImm + " to memory as it is too big"
                            );
                        }
                        
                        context.memory.write(
                            dstValue.read(context), // @dstAddress
                            srcImm
                        );
                        break;
                        
                    case REGISTER:
                        if (NumericValue.bitLength(srcImm) > dstValue.length) {
                            throw new RuntimeError(
                                "Cannot &mov immediate " + srcImm + " to variable "
                                + dstValue.getReferencedName() + " as it is too big"
                            );
                        }
                        
                        dstValue.write(srcImm, context);
                        break;
                        
                    case IMMEDIATE:
                        throw new RuntimeException("impossible");
                }
                break;
                
            case ADDRESS:
                BigInteger srcInteger = context.memory.read(srcValue.read(context)); // @srcAddress
                switch (dst) {
                    case ADDRESS:
                        context.memory.write(
                            dstValue.read(context), // @dstAddress
                            srcInteger
                        );
                        break;
                        
                    case REGISTER:
                        if (dstValue.length != context.memory.wordLength) {
                            throw new RuntimeError(
                                "Cannot &mov memory word to variable " + dstValue.getReferencedName()
                                + " as it has a different length"
                            );
                        }
                        
                        dstValue.write(srcInteger, context);
                        break;
                        
                    case IMMEDIATE:
                        throw new RuntimeException("impossible");
                }
                break;
                
            case REGISTER:
                switch (dst) {
                    case ADDRESS:
                        if (srcValue.length != context.memory.wordLength) {
                            throw new RuntimeError(
                                "Cannot &mov to memory from " + srcValue.getReferencedName()
                                + " as it has a different length"
                            );
                        }
                        
                        context.memory.write(
                            dstValue.read(context), // @dstAddress
                            srcValue.read(context)  // srcRegister
                        );
                        break;
                        
                    case REGISTER:
                        if (srcValue.length != dstValue.length) {
                            throw new RuntimeError(
                                "Cannot &mov between two variables " + srcValue.getReferencedName()
                                + " and " + dstValue.getReferencedName() + " that do not have the same length"
                            );
                        }
                        
                        dstValue.write(srcValue.read(context), context);
                        break;
                        
                    case IMMEDIATE:
                        throw new RuntimeException("impossible");
                }
                break;
        }
    }
    
    /**
     * Checks if the given value is a valid memory address; throws RuntimeError
     * otherwise.
     * 
     * @param address
     * @param context
     * @param direction "to" or "from" - used in error message
     * 
     * @throws RuntimeError
     */
    private static void checkAddress(NumericValue address, Context context, String direction) throws RuntimeError
    {
        if (address.length != context.memory.addressLength) {
            throw new RuntimeError(
                "Cannot &mov " + direction + " memory address given in "
                + address.getReferencedName() + " as it doesn't have the proper length for an address"
            );
        }
    }
}
