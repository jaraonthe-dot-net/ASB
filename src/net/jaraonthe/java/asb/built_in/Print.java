package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;

/**
 * The {@code &print} and {@code &println} built-in functions.
 * 
 * These functions can be invoked in userland as well.<br>
 * <br>
 * {@code &print register};<br>
 * {@code &print immediate};<br>
 * {@code &print @addressReg};<br>
 * {@code &print @addressImm};<br>
 * {@code &print string};<br>
 * {@code &println register};<br>
 * {@code &println immediate};<br>
 * {@code &println @addressReg};<br>
 * {@code &println @addressImm};<br>
 * {@code &println string};<br>
 * {@code &println};<br>
 * <br>
 * {@code @addressReg} and {@code @addressImm} are used to print a memory cell
 * at the address given in a register or immediate.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Print implements Interpretable
{
    public enum Type
    {
        PRINT  ("&print"),
        PRINTLN("&println");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    
    public enum OperandType
    {
        IMMEDIATE         (Variable.Type.IMMEDIATE, false),
        REGISTER          (Variable.Type.REGISTER,  false),
        ADDRESS_IMMEDIATE (Variable.Type.IMMEDIATE, true),
        ADDRESS_REGISTER  (Variable.Type.REGISTER,  true),
        STRING            (Variable.Type.STRING,    false),
        NONE              (null,                    false);
        
        public final Variable.Type variableType;
        public final boolean isAddress;
        
        private OperandType(Variable.Type type, boolean isAddress)
        {
            this.variableType = type;
            this.isAddress = isAddress;
        }
    }
    
    protected final Print.Type type;
    protected final Print.OperandType operand;
    
    
    /**
     * @param type    Selects the actual function
     * @param operand Selects the function variant (via the Operand type)
     */
    private Print(Print.Type type, Print.OperandType operand)
    {
        this.type    = type;
        this.operand = operand;
    }
    
    /**
     * Creates a {@code &print} or {@code &println} built-in function with the
     * given operand variant.
     * 
     * @param type    Selects the actual function
     * @param operand Selects the function variant (via the Operand type)
     * 
     * @return
     */
    public static BuiltInFunction create(Print.Type type, Print.OperandType operand)
    {
        if (operand == Print.OperandType.NONE && type != Print.Type.PRINTLN) {
            throw new IllegalArgumentException(
                "Cannot create &print function without operand"
            );
        }
        
        BuiltInFunction function = new BuiltInFunction(type.functionName, true);

        if (operand != Print.OperandType.NONE) {
            if (operand.isAddress) {
                function.addCommandSymbols("@");
            }
            function.addParameterByType(operand.variableType, "parameter");
        }
        
        function.setInterpretable(new Print(type, operand));
        return function;
    }

    
    @Override
    public void interpret(Context context) throws ConstraintException, RuntimeError
    {
        context.settings.printOccurred = true;
        switch (this.operand) {
            case IMMEDIATE:
            case REGISTER:
                // Always trigger a read on virtual registers and bitwise access
                System.out.print(BuiltInFunction.getNumericValue("parameter", context.frame).read(context));
                break;
                
            case ADDRESS_IMMEDIATE:
                System.out.print(Print.getValueViaAddressImm(context, this.type.functionName));
                break;
                
            case ADDRESS_REGISTER:
                System.out.print(Print.getValueViaAddressReg(context, this.type.functionName));
                break;
                
            case STRING:
                String text = BuiltInFunction.getValue("parameter", context.frame).toString();
                System.out.print(text);
                if (text.charAt(text.length() - 1) == '\n') {
                    context.settings.printOccurred = false;
                }
                break;
                
            case NONE:
                // nothing
                break;
        }
        if (this.type == Print.Type.PRINTLN) {
            System.out.println();
            context.settings.printOccurred = false;
        }
    }
    
    /**
     * Retrieves a value from memory, using an /immediate parameter named
     * "parameter" as the address.
     * 
     * @param context
     * @param functionName
     * 
     * @return
     * 
     * @throws ConstraintException
     * @throws RuntimeError
     */
    public static BigInteger getValueViaAddressImm(
        Context context,
        String functionName
    ) throws ConstraintException, RuntimeError {
        NumericValue address  = BuiltInFunction.getNumericValue("parameter", context.frame);
        BigInteger addressImm = address.read(context);
        
        if (NumericValue.bitLength(addressImm) > context.memory.wordLength) {
            throw new ConstraintException(
                "Cannot " + functionName + "memory at address given in "
                + address.getReferencedName()
                + " immediate as it doesn't have the proper length for an address"
            );
        }
        
        return context.memory.read(addressImm);
    }
    
    /**
     * Retrieves a value from memory, using a /variable parameter named
     * "parameter" as the address.
     * 
     * @param context
     * @param functionName
     * 
     * @return
     * 
     * @throws ConstraintException
     * @throws RuntimeError
     */
    public static BigInteger getValueViaAddressReg(
        Context context,
        String functionName
    ) throws ConstraintException, RuntimeError {
        NumericValue address = BuiltInFunction.getNumericValue("parameter", context.frame);
        if (address.length != context.memory.addressLength) {
            throw new ConstraintException(
                "Cannot " + functionName + "memory at address given in "
                + address.getReferencedName() + " as it doesn't have the proper length for an address"
            );
        }
        
        return context.memory.read(address.read(context));
    }
}
