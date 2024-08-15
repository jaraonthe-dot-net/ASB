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
        super(name, length, length);
        
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid register " + name + " length. Given value is: " + length
            );
        }
    }
    
    /**
     * @return The length of this register in bits.
     */
    public int getLength()
    {
        return this.minLength;
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
    
    @Override
    public boolean hasGroup(String group)
    {
        return this.groups.contains(group);
    }
    
    @Override
    public String toString()
    {
        String groupsString = "";
        if (!this.groups.isEmpty()) {
            for (String group : this.groups) {
                if (!groupsString.isEmpty()) {
                    groupsString += ", ";
                }
                groupsString += group;
            }
            groupsString = "(" + groupsString + ")";
        }
        
        return super.toString() + groupsString;
    }
}
