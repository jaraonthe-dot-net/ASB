package net.jaraonthe.java.asb.ast.command;

/**
 * A function, which is effectively pretty much the same as a Command.
 * However, semantically they are considered something else, and so this
 * subclass exists as an easy means to distinguish them.<br>
 * 
 * Use {@link Command#fromName()} as an easy means to create a Command or
 * Function as applicable.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
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
