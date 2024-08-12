package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A virtual register, as configured by the user. Can be used like a regular
 * register, but is more powerful.<br>
 * 
 * - Getting and setting operations are overloaded (via .get and .set
 *   implementations)<br>
 * - May or may not have its own storage (.store)
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class VirtualRegister extends Register
{
    /**
     * If storage is configured, this is its length. -1 otherwise.
     */
    private int storeLength = -1;
    
    private Implementation getterImplementation = null;
    private Implementation setterImplementation = null;
    
    
    /**
     * @param name
     * @param length
     */
    public VirtualRegister(String name, int length)
    {
        super(name, length);
    }
    
    /**
     * Sets this register's storage. Once this has been called once, this
     * register has storage.
     * 
     * @param length Storage size in bits.
     * @return
     */
    public VirtualRegister setStore(int length)
    {
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid register " + name + " store length. Given value is: " + length
            );
        }
        
        this.storeLength = length;
        return this;
    }
    
    /**
     * @return True if this virtual register has storage.
     */
    public boolean hasStore()
    {
        return this.storeLength > 0;
    }
    
    /**
     * @return The configured store length. -1 if no store has been configured.
     */
    public int getStoreLength()
    {
        return this.storeLength;
    }
    
    
    /**
     * Use this once to set this register's getter's implementation.
     * 
     * @param implementation
     * @return Fluent interface
     */
    public VirtualRegister setGetterImplementation(Implementation implementation)
    {
        if (this.getterImplementation != null) {
            throw new IllegalStateException("Cannot set getter implementation more than once");
        }
        this.getterImplementation = implementation;
        return this;
    }
    
    /**
     * @return This register's getter's implementation, or null if not set yet.
     */
    public Implementation getGetterImplementation()
    {
        return this.getterImplementation;
    }
    
    /**
     * Use this once to set this register's setter's implementation.
     * 
     * @param implementation
     * @return Fluent interface
     */
    public VirtualRegister setSetterImplementation(Implementation implementation)
    {
        if (this.setterImplementation != null) {
            throw new IllegalStateException("Cannot set setter implementation more than once");
        }
        this.setterImplementation = implementation;
        return this;
    }
    
    /**
     * @return This register's setter's implementation, or null if not set yet.
     */
    public Implementation getSetterImplementation()
    {
        return this.setterImplementation;
    }
    
    
    @Override
    public String toString()
    {
        return super.toString() + " (virtual)";
    }
}
