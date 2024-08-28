package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Variable;

/**
 * An invocation argument that contains a literal label name.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class LabelArgument extends Argument
{
    /**
     * The label name this contains.
     */
    public final String name;
    
    /**
     * The program position this label points to.
     */
    private int labelPosition = -1;

    /**
     * 
     * @param name The label name this contains
     */
    public LabelArgument(String name)
    {
        this.name = name;
    }
    
    /**
     * @return True if the label position is set
     */
    public boolean hasLabelPosition()
    {
        return this.labelPosition != -1;
    }
    
    /**
     * @return The program position this label points to, or -1 if not set yet
     */
    public int getLabelPosition()
    {
        if (this.labelPosition == -1) {
            throw new IllegalStateException("Label position is not set yet");
        }
        return this.labelPosition;
    }
    
    /**
     * Sets the label position, i.e. the program position this label points to.
     * 
     * @param labelPosition
     * 
     * @throws IllegalStateException    if label position is already set
     * @throws IllegalArgumentException if labelPosition is negative
     *                            
     */
    public void setLabelPosition(int labelPosition)
    {
        if (this.labelPosition != -1) {
            throw new IllegalStateException("Cannot set label position more than once");
        }
        if (labelPosition < 0) {
            throw new IllegalArgumentException("Label position must be positive, is " + labelPosition);
        }
        this.labelPosition = labelPosition;
    }

    @Override
    public Variable.Type getVariableType()
    {
        return Variable.Type.LABEL;
    }
    
    @Override
    public String toString()
    {
        return this.name;
    }
}
