package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Function;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.UserError;
import net.jaraonthe.java.asb.interpret.Frame;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.Value;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A built-in function, which is not defined in ASB source code provided by the
 * user, but instead fully hardcoded within this namespace.<br>
 * 
 * Each built-in function requires a specific {@link Interpretable} implemented
 * in Java (instead of comprising Invocations within an {@link Implementation}).<br>
 * 
 * The laid out structure has been chosen so that built-in functions are fully
 * implemented in one place (each in their own Interpretable class, and all
 * listed in {@link #initBuiltInFunctions()}).<br>
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class BuiltInFunction extends Function
{
    private final boolean isUserlandInvokable;
    private final boolean useCallerFrame;
    
    /**
     * @param name                The name of this function (i.e. the "&..."
     *                            word at the front)
     * @param isUserlandInvokable True if this function can be invoked within
     *                            userland. False: Can only be invoked within
     *                            an implementation.
     */
    public BuiltInFunction(String name, boolean isUserlandInvokable)
    {
        this(name, isUserlandInvokable, false);
    }
    
    /**
     * @param name                The name of this function (i.e. the "&..."
     *                            word at the front)
     * @param isUserlandInvokable True if this function can be invoked within
     *                            userland. False: Can only be invoked within
     *                            an implementation.
     * @param useCallerFrame      True if no new frame shall be created for this
     *                            function, instead it will use the caller's
     *                            frame as its own. This also means that
     *                            function arguments are not prepared for this
     *                            function.<br>
     *                            Default false.
     */
    public BuiltInFunction(String name, boolean isUserlandInvokable, boolean useCallerFrame)
    {
        super(name);
        this.isUserlandInvokable = isUserlandInvokable;
        this.useCallerFrame      = useCallerFrame;
    }

    @Override
    public boolean isUserlandInvokable()
    {
        return this.isUserlandInvokable;
    }
    
    @Override
    public boolean useCallerFrame()
    {
        return this.useCallerFrame;
    }
    
    
    /* Common stuff for any bult-in function class */

    /**
     * Type of an operand which is either /register or /immediate.<br>
     * 
     * May be used by any built-in function class.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum OperandType
    {
        IMMEDIATE (Variable.Type.IMMEDIATE),
        REGISTER  (Variable.Type.REGISTER);
        
        public final Variable.Type variableType;
        
        private OperandType(Variable.Type type)
        {
            this.variableType = type;
        }
    }
    
    /**
     * Adds a parameter to this built-in function based on the given type.
     * 
     * @param type
     * @param name
     * @return
     */
    public BuiltInFunction addParameterByType(BuiltInFunction.OperandType type, String name)
    {
        return this.addParameterByType(type.variableType, name);
    }
    
    /**
     * Adds a parameter to this built-in function based on the given type.
     * 
     * @param type
     * @param name
     * 
     * @return Fluent interface
     */
    public BuiltInFunction addParameterByType(Variable.Type type, String name)
    {
        switch (type) {
        case IMMEDIATE:
            this.addParameter(new Variable(
                Variable.Type.IMMEDIATE,
                name,
                Constraints.MAX_LENGTH
            ));
            break;
            
        case REGISTER:
            this.addParameter(new Variable(
                Variable.Type.REGISTER,
                name,
                Constraints.MIN_LENGTH,
                Constraints.MAX_LENGTH
            ));
            break;
            
        case LABEL:
            // i.e. a local label
            this.addParameter(new Variable(
                Variable.Type.LABEL,
                name,
                Variable.LOCAL_LABEL_LENGTH,
                true
            ));
            break;
            
        case STRING:
            this.addParameter(new Variable(
                Variable.Type.STRING,
                name
            ));
            break;
            
        default:
            throw new IllegalArgumentException("Cannot use type " + type);
        }
        
        return this;
    }
    
    /**
     * Executes {@code frame.getValue(variableName)}; if a
     * {@link ConstraintException} is thrown, it is transformed to a
     * RuntimeException (so that it is not marked as a {@link UserError}).
     * 
     * @param variableName
     * @param frame
     * 
     * @return
     * 
     * @throws RuntimeException
     */
    public static Value getValue(String variableName, Frame frame)
    {
        try {
            return frame.getValue(variableName);
        } catch (ConstraintException e) {
            throw new RuntimeException("Built-in cannot access variable " + variableName);
        }
    }
    
    /**
     * Executes {@code frame.getNumericValue(variableName)}; if a
     * {@link ConstraintException} is thrown, it is transformed to a
     * RuntimeException (so that it is not marked as a {@link UserError}).
     * 
     * @param variableName
     * @param frame
     * 
     * @return
     * @throws RuntimeException
     */
    public static NumericValue getNumericValue(String variableName, Frame frame)
    {
        try {
            return frame.getNumericValue(variableName);
        } catch (ConstraintException e) {
            throw new RuntimeException("Built-in cannot access variable " + variableName);
        }
    }
    
    
    /**
     * Initializes and adds all built-in functions to the given AST.
     * 
     * @param ast
     */
    public static void initBuiltInFunctions(AST ast)
    {
        // &add, &addc, &sub, &subc, &mul, &div, &rem
        for (Arithmetic.Type type : Arithmetic.Type.values()) {
            for (Arithmetic.Operands operands : Arithmetic.Operands.values()) {
                if (Arithmetic.isValidCombination(type, operands)) {
                    ast.addCommand(Arithmetic.create(type, operands));
                }
            }
        }
        
        // &and, &or, &xor
        for (Logical.Type type : Logical.Type.values()) {
            for (BuiltInFunction.OperandType src2Type : BuiltInFunction.OperandType.values()) {
                ast.addCommand(Logical.create(type, src2Type));
            }
        }
        
        // &assert
        for (Assert.Operator operator : Assert.Operator.values()) {
            for (boolean hasMessage : new boolean[]{false, true}) {
                ast.addCommand(Assert.create(
                    operator,
                    BuiltInFunction.OperandType.IMMEDIATE,
                    BuiltInFunction.OperandType.REGISTER,
                    hasMessage
                ));
                ast.addCommand(Assert.create(
                    operator,
                    BuiltInFunction.OperandType.REGISTER,
                    BuiltInFunction.OperandType.IMMEDIATE,
                    hasMessage
                ));
                ast.addCommand(Assert.create(
                    operator,
                    BuiltInFunction.OperandType.REGISTER,
                    BuiltInFunction.OperandType.REGISTER,
                    hasMessage
                ));
            }
        }
        
        // &get... (system info)
        for (SystemInfo.Type type : SystemInfo.Type.values()) {
            ast.addCommand(SystemInfo.create(type));
        }
        
        // &get_program_counter, &set_program_counter
        for (ProgramCounter.Type type : ProgramCounter.Type.values()) {
            ast.addCommand(ProgramCounter.create(type));
        }
        
        // &halt
        ast.addCommand(Halt.create());
        
        // &jump
        ast.addCommand(Jump.create());
        
        // &jumpif
        for (Jumpif.Operator operator : Jumpif.Operator.values()) {
            ast.addCommand(Jumpif.create(
                operator,
                BuiltInFunction.OperandType.IMMEDIATE,
                BuiltInFunction.OperandType.REGISTER
            ));
            ast.addCommand(Jumpif.create(
                operator,
                BuiltInFunction.OperandType.REGISTER,
                BuiltInFunction.OperandType.IMMEDIATE
            ));
            ast.addCommand(Jumpif.create(
                operator,
                BuiltInFunction.OperandType.REGISTER,
                BuiltInFunction.OperandType.REGISTER
            ));
        }
        
        // &length
        ast.addCommand(Length.create());
        
        // &mov
        for (Mov.OperandType src : Mov.OperandType.values()) {
            ast.addCommand(Mov.create(Mov.OperandType.ADDRESS, src));
            ast.addCommand(Mov.create(Mov.OperandType.REGISTER, src));
        }
        
        // &movif
        for (Compare.Operator operator : Compare.Operator.values()) {
            for (BuiltInFunction.OperandType a : BuiltInFunction.OperandType.values()) {
                for (BuiltInFunction.OperandType b : BuiltInFunction.OperandType.values()) {
                    if (a == b && b == BuiltInFunction.OperandType.IMMEDIATE) {
                        continue;
                    }
                    for (Mov.OperandType src : Mov.OperandType.values()) {
                        ast.addCommand(Movif.create(operator, a, b, Mov.OperandType.ADDRESS, src));
                        ast.addCommand(Movif.create(operator, a, b, Mov.OperandType.REGISTER, src));
                    }
                }
            }
        }
        
        // &normalize
        ast.addCommand(Normalize.create());
        
        // &not
        for (BuiltInFunction.OperandType src : BuiltInFunction.OperandType.values()) {
            ast.addCommand(Not.create(src));
        }
        
        // &print, &println
        for (Print.Type type : Print.Type.values()) {
            for (Print.OperandType operandType : Print.OperandType.values()) {
                if (type == Print.Type.PRINT && operandType == Print.OperandType.NONE) {
                    continue;
                }
                ast.addCommand(Print.create(type, operandType));
            }
        }
        
        // &print_*, &println_*
        for (PrintFormatted.Type type : PrintFormatted.Type.values()) {
            for (PrintFormatted.Format format : PrintFormatted.Format.values()) {
                for (PrintFormatted.OperandType operandType : PrintFormatted.OperandType.values()) {
                    ast.addCommand(PrintFormatted.create(type, format, operandType));
                }
            }
        }
        
        // &return
        ast.addCommand(Return.create());
        
        // &sign_extend
        ast.addCommand(SignExtend.create());
        
        // &zero_extend
        ast.addCommand(ZeroExtend.create());
    }
}
