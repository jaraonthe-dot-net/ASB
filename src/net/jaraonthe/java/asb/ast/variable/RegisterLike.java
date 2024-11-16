package net.jaraonthe.java.asb.ast.variable;

import java.util.HashMap;
import java.util.Map;

/**
 * Stuff that both {@link Register} and {@link LocalVariable} have in common,
 * which is mostly group-related stuff.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class RegisterLike extends Variable
{
    /**
     * Group name => position (in order in which groups are listed in this
     * variable)
     */
    protected Map<String, Integer> groups = HashMap.newHashMap(1);
    
    
    /**
     * Length value is not checked here. Please do that in the subclass
     * constructor.
     * 
     * @param name
     * @param length length in bits
     */
    protected RegisterLike(String name, int length)
    {
        super(name, length, length);
    }
    
    /**
     * @return The length of this variable in bits.<br>
     *         0 if length is determined dynamically (i.e. via a length variable
     *         for a local variable).
     */
    public int getLength()
    {
        return this.minLength;
    }
    
    /**
     * Adds a group to this variable. Multiple groups can be assigned to a
     * variable.
     * 
     * @param group
     * @return Fluent interface
     */
    public RegisterLike addGroup(String group)
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
