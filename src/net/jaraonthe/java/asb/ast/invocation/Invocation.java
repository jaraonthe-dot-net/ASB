package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.interpret.Interpretable;

/**
 * Either an Invocation of a command or some other special action. This is what
 * makes up command implementations as well as the userland program.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public interface Invocation extends Interpretable
{
    /**
     * @return True if this invocation has already been resolved, i.e. this
     *         invocation is ready to be used. Most often this means the command
     *         that's being invoked is known. Additionally, label names have
     *         been resolved.
     */
    public boolean isResolved();

    /**
     * Resolves this invocation. I.e. figures out (most of) the missing
     * information in order to be able to intepret this invocation.<br>
     * 
     * Label names are resolved in an extra step, via {@link #resolveLabelNames()}.
     * 
     * @param ast
     * @param implementation The implementation which this Invocation is a part
     *                       of. Null: This invocation is part of userland code,
     *                       thus other rules apply.
     *                       
     * @return Fluent interface
     * 
     * @throws ConstraintException
     */
    public Invocation resolve(AST ast, Implementation implementation) throws ConstraintException;
    
    /**
     * Resolves label names that are used as arguments. This is an extra step
     * expected to be done after all parsing is done (because a label can be
     * anywhere).
     * 
     * @param ast
     * @param implementation The implementation which this Invocation is a part
     *                       of. Null: This invocation is part of userland code.
     * 
     * @return Fluent interface
     * 
     * @throws ConstraintException
     */
    public Invocation resolveLabelNames(AST ast, Implementation implementation) throws ConstraintException;
}
