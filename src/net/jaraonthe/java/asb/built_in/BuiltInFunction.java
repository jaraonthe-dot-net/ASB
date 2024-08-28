package net.jaraonthe.java.asb.built_in;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Function;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.interpret.Interpretable;

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
    
    /**
     * @param name                The name of this function (i.e. the "&..."
     *                            word at the front)
     * @param isUserlandInvokable True if this function can be invoked within
     *                            userland. False: Can only be invoked within
     *                            an implementation.
     */
    public BuiltInFunction(String name, boolean isUserlandInvokable)
    {
        super(name);
        this.isUserlandInvokable = isUserlandInvokable;
    }

    @Override
    public boolean isUserlandInvokable()
    {
        return this.isUserlandInvokable;
    }
    
    
    /**
     * Inits and adds all built-in functions to the given AST.
     * 
     * @param ast
     */
    public static void initBuiltInFunctions(AST ast)
    {
        // TODO Add: &zero_extend, multiplication, division, remainder
        
        // &add, &addc, &sub, &subc
        for (Add.Type type : Add.Type.values()) {
            for (Add.Operands operands : Add.Operands.values()) {
                if (Add.isValidCombination(type, operands)) {
                    ast.addCommand(Add.create(type, operands));
                }
            }
        }
        
        // &and, &or, &xor
        for (Logical.Type type : Logical.Type.values()) {
            for (Logical.Operands operands : Logical.Operands.values()) {
                ast.addCommand(Logical.create(type, operands));
            }
        }
        
        // &assert
        for (Assert.Operator operator : Assert.Operator.values()) {
            for (boolean hasMessage : new boolean[]{false, true}) {
                ast.addCommand(Assert.create(operator, Assert.OperandType.IMMEDIATE, Assert.OperandType.REGISTER,  hasMessage));
                ast.addCommand(Assert.create(operator, Assert.OperandType.REGISTER,  Assert.OperandType.IMMEDIATE, hasMessage));
                ast.addCommand(Assert.create(operator, Assert.OperandType.REGISTER,  Assert.OperandType.REGISTER,  hasMessage));
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
        
        // &jump
        ast.addCommand(Jump.create());
        
        // &jumpif
        for (Jumpif.Operator operator : Jumpif.Operator.values()) {
            ast.addCommand(Jumpif.create(operator, Jumpif.OperandType.IMMEDIATE, Jumpif.OperandType.REGISTER));
            ast.addCommand(Jumpif.create(operator, Jumpif.OperandType.REGISTER,  Jumpif.OperandType.IMMEDIATE));
            ast.addCommand(Jumpif.create(operator, Jumpif.OperandType.REGISTER,  Jumpif.OperandType.REGISTER));
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
            for (Compare.OperandType a : Compare.OperandType.values()) {
                for (Compare.OperandType b : Compare.OperandType.values()) {
                    if (a == b && b == Compare.OperandType.IMMEDIATE) {
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
        for (Not.Operand operand : Not.Operand.values()) {
            ast.addCommand(Not.create(operand));
        }
        
        // &print, &println
        for (Print.Type type : Print.Type.values()) {
            ast.addCommand(Print.create(type, Print.Operand.REGISTER));
            ast.addCommand(Print.create(type, Print.Operand.IMMEDIATE));
            ast.addCommand(Print.create(type, Print.Operand.STRING));
        }
        ast.addCommand(Print.create(Print.Type.PRINTLN, Print.Operand.NONE));
        
        // &print_*, &println_*
        for (PrintFormatted.Type type : PrintFormatted.Type.values()) {
            for (PrintFormatted.Format format : PrintFormatted.Format.values()) {
                ast.addCommand(PrintFormatted.create(type, format));
            }
        }
        
        // &sign_extend
        ast.addCommand(SignExtend.create());
    }
}
