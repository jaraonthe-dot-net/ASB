package net.jaraonthe.java.asb.ast.variable;

import java.util.HashSet;
import java.util.Set;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A (global) register, as configured by the user. Has a name, length, and
 * optionally some groups (which control which commands this register can be
 * used with).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Register extends VariableLike
{
    private Set<String> groups = HashSet.newHashSet(2);
    
    
    /**
     * @param name
     * @param length length in bits
     */
    public Register(String name, int length)
    {
        super(name, length);
        
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid register " + name + " length. Given value is: " + length
            );
        }
    }
    
    /**
     * Adds a group to this register. Multiple groups can be assigned to a
     * register.
     * 
     * @param group
     * @return Fluent interface
     */
    public Register addGroup(String group)
    {
        this.groups.add(group);
        return this;
    }
    
    /**
     * @param group
     * @return True if this register has the given group assigned.
     */
    public boolean hasGroup(String group)
    {
        return this.groups.contains(group);
    }
}
