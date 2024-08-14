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
        // TODO Add more
        
        // &add
        ast.addCommand(Add.create(Add.Variant.REG_REG_IMM, false));
        ast.addCommand(Add.create(Add.Variant.REG_REG_REG, false));
        
        // &addc
        ast.addCommand(Add.create(Add.Variant.REG_REG_IMM, true));
        ast.addCommand(Add.create(Add.Variant.REG_REG_REG, true));
        
        // &assert
        for (Assert.Operator operator : Assert.Operator.values()) {
            for (boolean hasMessage : new boolean[]{false, true}) {
                ast.addCommand(Assert.create(operator, Assert.OperandType.IMM, Assert.OperandType.REG, hasMessage));
                ast.addCommand(Assert.create(operator, Assert.OperandType.REG, Assert.OperandType.IMM, hasMessage));
                ast.addCommand(Assert.create(operator, Assert.OperandType.REG, Assert.OperandType.REG, hasMessage));
            }
        }
        
        // &length
        ast.addCommand(Length.create());
        
        // &mov
        ast.addCommand(Mov.create(Mov.Variant.MEM_IMM));
        ast.addCommand(Mov.create(Mov.Variant.MEM_MEM));
        ast.addCommand(Mov.create(Mov.Variant.MEM_REG));
        ast.addCommand(Mov.create(Mov.Variant.REG_IMM));
        ast.addCommand(Mov.create(Mov.Variant.REG_MEM));
        ast.addCommand(Mov.create(Mov.Variant.REG_REG));
        
        // &sign_extend
        ast.addCommand(SignExtend.create());
    }
}
