package net.jaraonthe.java.asb.interpret.value;

import net.jaraonthe.java.asb.ast.invocation.LabelArgument;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.interpret.Frame;

/**
 * The value of a label parameter. This is only used for local label parameters
 * - otherwise a {@link NumericValue} is used to represent the userland label
 * position.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class LabelValue extends Value
{
    /**
     * The argument this value refers to (contains all required information).
     */
    public final LabelArgument argument;
    
    /**
     * The frame to which this label applies. I.e. any program counter changes
     * (e.g. from &jump) shall affect this frame.
     */
    public final Frame frame;
    
    /**
     * @param variable the Variable which this Value is assigned to
     * @param argument the argument that is used to fill this parameter
     * @param frame    The frame to which this label applies. I.e. any program
     *                 counter changes (e.g. from &jump) shall affect this frame.
     */
    public LabelValue(Variable variable, LabelArgument argument, Frame frame)
    {
        super(variable);
        this.argument = argument;
        this.frame    = frame;
    }
    
    @Override
    public String toString()
    {
        return this.argument.toString();
    }
}
