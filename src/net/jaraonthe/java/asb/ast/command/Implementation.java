package net.jaraonthe.java.asb.ast.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.invocation.LocalVariableInitialization;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;

/**
 * This is the implementation of a command or function, which is made up of
 * Invocations as defined in ASB source code. It contains the program that is
 * executed when the command is invoked.<br>
 * 
 * Iterating this iterates over the Invocations that make up the program.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Implementation implements Interpretable, Iterable<Invocation>
{
    /**
     * Local variables and command parameters, accessed via their name.
     */
    private Map<String, Variable> variables = HashMap.newHashMap(4);
    
    private List<Invocation> program = new ArrayList<>(4);
    
    /**
     * Label name => program position the label points to
     */
    private Map<String, Integer> labels = HashMap.newHashMap(1);
    
    
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
     * Adds a local variable. This also adds the required
     * LocalVariableInitialization to this program.
     * 
     * @param variable of type LOCAL_VARIABLE
     * @return Fluent interface
     * @throws ConstraintException if adding the LocalVariableInitialization
     *                             would exceed maximum allowed program size
     */
    public Implementation addLocalVariable(Variable localVariable) throws ConstraintException
    {
        if (this.variables.containsKey(localVariable.name)) {
            throw new IllegalArgumentException(
                "Cannot add same variable " + localVariable.name + " more than once"
            );
        }
        
        this.variables.put(localVariable.name, localVariable);
        this.add(new LocalVariableInitialization(localVariable));
        
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
    
    
    /**
     * Adds a label pointing to the next program position in this implementation.
     * 
     * @param labelName
     * @return Fluent interface
     */
    public Implementation addLabel(String labelName)
    {
        if (this.labels.containsKey(labelName)) {
            throw new IllegalArgumentException(
                "Cannot add same label " + labelName + " more than once"
            );
        }
        
        this.labels.put(labelName, this.program.size());
        return this;
    }
    
    /**
     * @param labelName
     * @return True if a label with this name exists.
     */
    public boolean labelExists(String labelName)
    {
        return this.labels.containsKey(labelName);
    }
    
    /**
     * @param labelName
     * @return The position this label points to, or -1 if the label doesn't
     *         exist.
     */
    public int getLabel(String labelName)
    {
        Integer position = this.labels.get(labelName);
        if (position == null) {
            return -1;
        }
        return position.intValue();
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

    @Override
    public void interpret(Context context) throws RuntimeError
    {
        while (true) {
            Invocation invocation;
            try {
                invocation = this.program.get(context.frame.programCounter);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            context.frame.programCounter++;
            
            // This may modify the program counter
            invocation.interpret(context);
        }
    }
}
