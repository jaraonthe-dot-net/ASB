package net.jaraonthe.java.asb.ast.command;

/**
 * A (built-in) function.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
// TODO Consider using a dedicated class for built-in functions (which handles
//      implementation differently).
public class Function extends Command
{
    /**
     * @param name
     */
    public Function(String name)
    {
        super(name);
    }

    @Override
    public boolean isUserlandInvokable()
    {
        return false;
    }
}
