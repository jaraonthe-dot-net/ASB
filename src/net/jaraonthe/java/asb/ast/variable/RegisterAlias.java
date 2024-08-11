package net.jaraonthe.java.asb.ast.variable;

/**
 * An alias for a register, as configured by the user. Can be used like a
 * regular register, but uses the same storage as the aliased register.<br>
 * 
 * Has its own group settings, i.e. the alias and the aliased register can have
 * different groups.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class RegisterAlias extends Register
{
    /**
     * The register this is an alias for.
     */
    public Register aliasedRegister;
    
    /**
     * @param name
     * @param aliasedRegister
     */
    public RegisterAlias(String name, Register aliasedRegister)
    {
        super(name, aliasedRegister.length);
        while (aliasedRegister instanceof RegisterAlias) {
            // Prevent chain of aliases pointing to aliases
            aliasedRegister = ((RegisterAlias)aliasedRegister).aliasedRegister;
        }
        
        this.aliasedRegister = aliasedRegister;
    }
    
    @Override
    public String toString()
    {
        return super.toString() + " aliases " + this.aliasedRegister.name;
    }
}
