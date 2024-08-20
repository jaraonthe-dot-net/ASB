package net.jaraonthe.java.asb.interpret;

import net.jaraonthe.java.asb.ast.AST;

/**
 * Gives context to {@link Interpretable}s. This should be all that is required
 * to interpret any Interpretable.
 * 
 * @see Interpretable#interpret(Context)
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Context
{
    /**
     * The frame containing the current variable scope.
     */
    public final Frame frame;
    
    /**
     * The AST of the program being executed.
     */
    public final AST ast;

    /**
     * @param frame
     * @param ast
     */
    public Context(Frame frame, AST ast)
    {
        this.frame = frame;
        this.ast   = ast;
    }
}
