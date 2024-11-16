package net.jaraonthe.java.asb.ast;

import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.invocation.CommandInvocation;
import net.jaraonthe.java.asb.ast.variable.Parameter;

/**
 * Stuff that both {@link Command} and {@link CommandInvocation} have in common.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class CommandLike
{
    public final String name;
    
    /**
     * A variant of the command signature: This is used to group similar command
     * variants together (via {@link #getResolvingClass()}. This excludes
     * parameter details like length and group, which are checked in an extra
     * resolving step.
     */
    private String resolvingSignature = "";
    
    
    /**
     * @param name
     */
    public CommandLike(String name)
    {
        this.name = name;
    }

    /**
     * Adds command symbols to the resolving signature.
     * 
     * @param symbols
     */
    protected void addCommandSymbolsToResolvingSignature(String symbols)
    {
        this.resolvingSignature += symbols;
    }
    
    /**
     * Adds a parameter to the resolving signature.
     * 
     * @param type Here we only care about the parameter or argument type (as
     *             details are ignored).
     */
    protected void addParameterToResolvingSignature(Parameter.Type type)
    {
        this.resolvingSignature += type.signatureMarker;
    }
    
    /**
     * A command's resolving class is used to group similar command variants
     * together. All commands with the same class as an invocation are
     * candidates for resolving that particular invocation to a specific
     * command.
     * 
     * @return
     */
    public String getResolvingClass()
    {
        return this.name + " " + this.resolvingSignature;
    }
}
