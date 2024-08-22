package net.jaraonthe.java.asb.interpret;

import java.util.HashMap;
import java.util.Map;

import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.Value;

/**
 * This contains variable values for the global or a local scope.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Frame
{
    /**
     * This is used to have access to the global scope. Null: This frame is the
     * global scope.
     */
    public final Frame parentFrame;
    
    /**
     * variable name => value
     */
    private Map<String, Value> values = HashMap.newHashMap(4);

    
    /**
     * Creates an empty global frame.
     */
    public Frame()
    {
        this(null);
    }
    
    /**
     * Creates an empty local frame.
     * 
     * @param parentFrame the global frame
     */
    public Frame(Frame parentFrame)
    {
        this.parentFrame = parentFrame;
    }
    
    /**
     * @return the topmost parent frame
     */
    public Frame getRootParentFrame()
    {
        if (this.parentFrame == null) {
            return this;
        }
        return this.parentFrame.getRootParentFrame();
    }
    
    /**
     * Adds a variable's value instance.
     * 
     * @param value
     * @return Fluent interface
     */
    public Frame addValue(Value value)
    {
        if (this.values.containsKey(value.variable.name)) {
            throw new IllegalArgumentException(
                "Cannot add value for same variable name " + value.variable.name + " more than once"
            );
        }
        this.values.put(value.variable.name, value);
        return this;
    }
    
    /**
     * @param variableName
     * @return true if a value with the given variableName exists (either here
     *         or in the parent frame)
     */
    public boolean valueExists(String variableName)
    {
        if (this.values.containsKey(variableName)) {
            return true;
        }
        if (this.parentFrame != null) {
            return this.parentFrame.valueExists(variableName);
        }
        return false;
    }
    
    /**
     * @param variableName
     * @return Value with the given variableName, or null if it doesn't exist
     *         (neither here nor in the parent frame)
     */
    public Value getValue(String variableName)
    {
        Value value = this.values.get(variableName);
        if (value == null && this.parentFrame != null) {
            return this.parentFrame.getValue(variableName);
        }
        return value;
    }
    
    /**
     * Shortcut for {@code (NumericValue) frame.getValue(variableName)}.<br>
     * 
     * This should only be used when a value is expected to be of type {@link
     * NumericValue}.
     * 
     * @param variableName
     * @return NumericValue with the given variableName, or null if it doesn't
     *         exist (neither here nor in the parent frame)
     */
    public NumericValue getNumericValue(String variableName)
    {
        return (NumericValue) this.getValue(variableName);
    }
}
