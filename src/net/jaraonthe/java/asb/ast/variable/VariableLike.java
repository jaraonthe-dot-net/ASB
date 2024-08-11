package net.jaraonthe.java.asb.ast.variable;

/**
 * Stuff that both {@link Variable} and {@link Register} have in common.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class VariableLike
{
    public final String name;
    
    /**
     * The length in bits. -1 if this setting is not applicable for this
     * variable type (as defined by subclasses).
     */
    public final int length;
    
    /**
     * @param name
     * @param length This value is not checked here. Please do that in the
     *               subclass constructor.
     */
    protected VariableLike(String name, int length)
    {
        this.name   = name;
        this.length = length;
    }
    
    @Override
    public String toString()
    {
        return this.name + (this.length != -1 ? "''" + this.length : "");
    }
}
