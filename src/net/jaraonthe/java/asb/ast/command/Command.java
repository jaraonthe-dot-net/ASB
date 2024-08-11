package net.jaraonthe.java.asb.ast.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jaraonthe.java.asb.ast.CommandLike;
import net.jaraonthe.java.asb.ast.variable.Variable;

/**
 * A command, as declared using the ".define" directive.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Command extends CommandLike
{
    /**
     * A technical (unreadable) representation of the command symbols and
     * parameters that make up this Command. Together with the Command name this
     * constitutes the Command's identity.
     */
    private String signature = "";
    
    /**
     * A human-readable variant of the signature.
     */
    private String readableSignature = "";
    
    /**
     * Command parameters, in the order in which they appear in the signature.
     */
    private List<Variable> parameters = new ArrayList<>(3);
    
    private Implementation implementation = null;
    
    
    /**
     * @param name
     */
    public Command(String name)
    {
        super(name);
    }
    
    /**
     * Creates a Command or a Function entity based on the given Command or
     * Function name.
     * 
     * @param name at least 1 char long. If first char == '&', a Function
     *             entity will be created.
     * @return Command or Function entity.
     */
    public static Command fromName(String name)
    {
        if (name.charAt(0) == '&') {
            return new Function(name);
        }
        return new Command(name);
    }

    /**
     * Adds command symbols to this command's signature.<br>
     * 
     * Together with {@link #addParameter()}, this must be invoked in the
     * order in which these Tokens appear in the ASB source code.
     * 
     * @param symbols
     */
    public void addCommandSymbols(String symbols)
    {
        this.signature         += symbols;
        this.readableSignature += symbols;
        this.addCommandSymbolsToResolvingSignature(symbols);
    }
    
    /**
     * Adds a parameter to this command's signature.<br>
     * 
     * Together with {@link #addCommandSymbols()}, this must be invoked in the
     * order in which these Tokens appear in the ASB source code.
     * 
     * @param parameter
     * @return Fluent interface
     */
    public Command addParameter(Variable parameter)
    {
        this.signature += parameter.type.signatureMarker;
        if (
            !this.readableSignature.isEmpty()
            && this.readableSignature.charAt(this.readableSignature.length() - 1) != ' '
        ) {
            this.readableSignature += " ";
        }
        this.readableSignature += parameter.type.readableSignaturePlaceholder;
        
        // Length & group details
        boolean hasDetails = false;
        if (parameter.type.hasLength) {
            hasDetails              = true;
            this.signature         += parameter.length;
            this.readableSignature += "''" + parameter.length;
        }
        if (parameter.hasGroup()) {
            hasDetails              = true;
            this.signature         += parameter.getGroup();
            this.readableSignature += "(" + parameter.getGroup() + ")";
        }
        if (hasDetails) {
            this.signature += Variable.Type.END_OF_MARKER_DETAILS;
        }
        this.readableSignature += " ";
        
        this.parameters.add(parameter);
        
        this.addParameterToResolvingSignature(parameter.type);
        
        return this;
    }
    
    /**
     * @return Readonly parameter list
     */
    public List<Variable> getParameters()
    {
        return Collections.unmodifiableList(this.parameters);
    }
    
    /**
     * Use this once to set this command's implementation.
     * 
     * @param implementation
     * @return Fluent interface
     */
    public Command setImplementation(Implementation implementation)
    {
        if (this.implementation != null) {
            throw new IllegalStateException("Cannot set implementation more than once");
        }
        this.implementation = implementation;
        return this;
    }
    
    /**
     * @return This command's implementation, or null if not set yet.
     */
    public Implementation getImplementation()
    {
        return this.implementation;
    }
    
    
    /**
     * The command identity uniquely identifies a command (incl. differentiating
     * between different variants of one command - each variant has a unique
     * identity). This is a technical identifier that is not human-readable.
     * 
     * @link #getReadableIdentity()}
     * @return
     */
    public String getIdentity()
    {
        return this.name + " " + this.signature;
    }
    
    /**
     * @see #getIdentity()
     * @return A human-readable variant of the command identity.
     */
    public String getReadableIdentity()
    {
        return this.name + " " + this.readableSignature;
    }
    
    @Override
    public String toString()
    {
        return this.getReadableIdentity();
    }
    
    /**
     * @return True if this command or function can be invoked within the user
     *         language (in userland). False: Can only be invoked within an
     *         implementation.<br>
     *         True for all commands (as they make up the user language),
     *         generally false for functions (except a handful of built-in
     *         functions).
     */
    public boolean isUserlandInvokable()
    {
        return true;
    }
}
