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
     * The memory of the virtual system. May be null (if no memory is configured).
     */
    public final Memory memory;
    
    /**
     * The AST of the program being executed.
     */
    public final AST ast;

    /**
     * @param frame
     * @param memory May be null
     * @param ast
     */
    public Context(Frame frame, Memory memory, AST ast)
    {
        this.frame  = frame;
        this.memory = memory;
        this.ast    = ast;
    }
    
    /**
     * Creates a new Context which is identical to this context except that the
     * frame has been replaced with the given frame.
     * 
     * @param frame
     * @return
     */
    public Context withFrame(Frame frame)
    {
        return new Context(frame, this.memory, this.ast);
    }
}
