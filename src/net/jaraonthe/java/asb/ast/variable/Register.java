package net.jaraonthe.java.asb.ast.variable;

import java.util.HashMap;
import java.util.Map;

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
    /**
     * Group name => position (in order in which groups are listed in this
     * register)
     */
    private Map<String, Integer> groups = HashMap.newHashMap(1);
    
    
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
        if (this.groups.containsKey(group)) {
            throw new IllegalArgumentException(
                "Cannot add same group " + group + " more than once"
            );
        }
        
        this.groups.put(group, this.groups.size());
        return this;
    }
    
    @Override
    public boolean hasGroup(String group)
    {
        return this.groups.containsKey(group);
    }
    
    /**
     * @param group
     * @return The position at which this group was listed
     */
    public int getGroupPosition(String group)
    {
        Integer position = this.groups.get(group);
        if (position == null) {
            return -1;
        }
        return position.intValue();
    }
    
    @Override
    public String toString()
    {
        String groupsString = "";
        if (!this.groups.isEmpty()) {
            for (String group : this.groups.keySet()) {
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
