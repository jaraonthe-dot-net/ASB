package net.jaraonthe.java.asb.ast.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;

/**
 * This is the implementation of a command or function. It contains the program
 * that is executed when the command is invoked.<br>
 * 
 * Iterating this iterates over the Invocations that make up the program.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Implementation implements Iterable<Invocation>
{
    /**
     * Local variables and command parameters, accessed via their name.
     */
    private Map<String, Variable> variables = HashMap.newHashMap(4);
    
    private List<Invocation> program = new ArrayList<>(4);
    
    
    /**
     * @param parameters The parameters of the containing command. May be null
     */
    public Implementation(List<Variable> parameters)
    {
        if (parameters != null) {
            for (Variable p: parameters) {
                this.variables.put(p.name, p);
            }
        }
    }

    /**
     * Adds a local variable.<br>
     * 
     * If this implementation already has a variable with the same name it will
     * be overwritten - this may not be desired, use {@link #variableExists()}
     * to prevent this.
     * 
     * @param variable
     * @return Fluent interface
     */
    public Implementation addVariable(Variable variable)
    {
        this.variables.put(variable.name, variable);
        return this;
    }
    
    /**
     * @param variableName
     * @return True if a local variable or parameter with the given name exists.
     */
    public boolean variableExists(String variableName)
    {
        return this.variables.containsKey(variableName);
    }
    
    /**
     * @param variableName
     * @return Local variable or parameter with the given name, or null if it
     *         doesn't exist
     */
    public Variable getVariable(String variableName)
    {
        return this.variables.get(variableName);
    }
    
    
    /**
     * Adds an invocation to this implementation program.
     * 
     * @param item
     * @return Fluent interface
     * @throws ConstraintException if adding this invocation would exceed
     *                             maximum allowed program size
     */
    public Implementation add(Invocation item) throws ConstraintException
    {
        if (this.program.size() >= Integer.MAX_VALUE) {
            throw new ConstraintException(
                "Cannot have more than " + Integer.MAX_VALUE + " machine commands"
            );
        }
        this.program.add(item);
        return this;
    }
    
    /**
     * @return true if this implementation contains no Invocation
     */
    public boolean isEmpty()
    {
        return this.program.isEmpty();
    }
    
    @Override
    public Iterator<Invocation> iterator()
    {
        return this.program.iterator();
    }
    
    @Override
    public String toString()
    {
        String text = "";
        for (Invocation i : this.program) {
            text += i + "\n";
        }
        
        return this.variables + "\n" + text;
    }
}