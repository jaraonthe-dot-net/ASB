package net.jaraonthe.java.asb.ast.invocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.CommandLike;
import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.ConstraintException;

/**
 * An Invocation of a command. This is what makes up the userland program.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Invocation extends CommandLike
{
    /**
     * This human-readable signature is only used when this Invocation is not
     * yet resolved.
     */
    private String readableSignature = "";
    
    /**
     * Once the referenced command has been resolved ({@link #resolve()}), this
     * points to the command that's being invoked. Otherwise this is null.
     */
    private Command invokedCommand = null;
    
    /**
     * Once the referenced command has been resolved ({@link #resolve()}), this
     * contains the actual arguments (which will be used by the interpreter).<br>
     * Before that, this contains preliminary arguments, which may include
     * {@link RawArgument}s which are used for and processed when resolving.
     */
    private List<Argument> arguments = new ArrayList<>(3);
    
    
    /**
     * @param name of the invoked command
     */
    public Invocation(String name)
    {
        super(name);
    }
    
    
    /**
     * @return True if this invocation has already been resolved, i.e. the
     *         command that's being invoked is known.
     */
    public boolean isResolved()
    {
        return this.invokedCommand != null;
    }
    
    /**
     * @return The command that's being invoked. Null if it hasn't been resolved
     *         yet.
     */
    public Command getInvokedCommand()
    {
        return this.invokedCommand;
    }
    // TODO getter for arguments - or an iterator instead?
    
    /**
     * Adds command symbols to this invocation's signature.<br>
     * 
     * Together with {@link #addArgument()}, this must be invoked in the
     * order in which these Tokens appear in the ASB source code.
     * 
     * @param symbols
     */
    public void addCommandSymbols(String symbols)
    {
        this.readableSignature += symbols;
        this.addCommandSymbolsToResolvingSignature(symbols);
    }
    
    /**
     * Adds an argument to this invocation.<br>
     * 
     * Together with {@link #addCommandSymbols()}, this must be invoked in the
     * order in which they appear in the ASB source code.
     * 
     * @param argument May be a RawArgument as well.
     */
    public void addArgument(Argument argument)
    {
        if (
            !this.readableSignature.isEmpty()
            && this.readableSignature.charAt(this.readableSignature.length() - 1) != ' '
        ) {
            this.readableSignature += " ";
        }
        this.readableSignature += argument;
        this.readableSignature += " ";
        
        this.addParameterToResolvingSignature(argument.getVariableType());
        this.arguments.add(argument);
    }
    
    
    /**
     * Resolves this invocation. I.e. figures out which command is being invoked
     * and conforms the arguments.
     * 
     * @param ast
     * @param implementation The implementation which this Invocation is a part
     *                       of. Null: This invocation is part of userland code,
     *                       thus other rules apply.
     *                       
     * @return Fluent interface
     * 
     * @throws ConstraintException if no fitting command can be found, or a
     *                             function is invoked from userland.
     */
    public Invocation resolve(AST ast, Implementation implementation) throws ConstraintException
    {
        if (this.invokedCommand != null) {
            throw new IllegalStateException(
                "Attempting to resolve an Invocation that has already been resolved"
            );
        }
        
        Set<Command> commandClass = ast.getCommandClass(this.getResolvingClass());
        if (commandClass == null || commandClass.isEmpty()) {
            throw new ConstraintException("No command found for Invocation " + this);
        }
        
        // viable commands (all these fit the invocation signature & arguments)
        List<Command> viableCommands = new ArrayList<>(commandClass.size());
        for (Command c : commandClass) {
            if (this.isCommandViable(c, ast, implementation)) {
                viableCommands.add(c);
            }
        }
        if (viableCommands.isEmpty()) {
            // TODO this error could be due to using registers/vars that don't
            //      exist - can that be detected somehow, and a more appropriate
            //      error message be given in that case? Or at least point out
            //      this potential root cause in the existing message.
            throw new ConstraintException("No command found for Invocation " + this);
        }
        
        if (viableCommands.size() > 1) {
            // select best command
            // TODO choose a command variant (apply tie-breakers via sorting)
            throw new RuntimeException(
                "Resolving against multiple viable Command Variants not implemented yet"
            );
        }
        
        this.invokedCommand = viableCommands.getFirst();
        this.conformArgs(ast, implementation);
        
        if (implementation == null && !this.invokedCommand.isUserlandInvokable()) {
            throw new ConstraintException(
                "Attempting to invoke Function " + this.invokedCommand
                + " which can only be called from within a command implementation"
            );
        }
        
        return this;
    }
    
    /**
     * @param command        Must have the same resolving class as this
     *                       invocation
     * @param ast
     * @param implementation The implementation containing this invocation. Null
     *                       if within userland code.
     * 
     * @return True if command is viable for this invocation, i.e. it fits all
     *         invocation arguments.
     */
    private boolean isCommandViable(Command command, AST ast, Implementation implementation)
    {
        Iterator<Variable> iter = command.getParameters().iterator();
        for (Argument argument : this.arguments) {
            Variable parameter = iter.next();
            
            if (argument instanceof RawArgument) {
            // INV: register/var/label
                RawArgument ra = (RawArgument) argument;
                switch (parameter.type) {
                    case REGISTER:
                    // CMD: register/var
                        if (!ra.potentialRegister.isNumeric()) {
                            // i.e. this is a label or string parameter
                            return false;
                        }
                        
                        if (
                            parameter.hasGroup()
                            && !ra.potentialRegister.hasGroup(parameter.getGroup())
                        ) {
                            // group doesn't fit
                            return false;
                        }
                        if (
                            ra.potentialRegister.minLength > 0 // don't check dynamic length (length == 0)
                            && (
                                // at least one possible length has to fit
                                // (rest is checked dynamically at runtime)
                                ra.potentialRegister.maxLength < parameter.minLength
                                || ra.potentialRegister.minLength > parameter.maxLength
                            )
                        ) {
                            return false;
                        }
                        break;

                    // TODO support label (i.e. either a Label Parameter, or
                    //      a label literal)
                    
                    case STRING:
                    // CMD: string
                        if (
                            !(ra.potentialRegister instanceof Variable)
                            || ((Variable)ra.potentialRegister).type != Variable.Type.STRING
                        ) {
                            // no local var found or incorrect type
                            return false;
                        }
                        break;
                        
                    default:
                        return false;
                }
                
            } else if (argument instanceof RegisterArgument) {
            // INV: register/var '...
                if (parameter.type != Variable.Type.REGISTER) {
                    return false;
                }
                // CMD: register/var
                
                RegisterArgument ra = (RegisterArgument) argument;
                // referenced variable is already known
                // - and we assume it is a numeric variable (otherwise this
                //   would be a RawArgument)
                if (
                    parameter.hasGroup()
                    && !ra.register.hasGroup(parameter.getGroup())
                ) {
                    // group doesn't fit
                    return false;
                }
                if (
                    // at least one possible length has to fit
                    // (rest is checked dynamically at runtime)
                    ra.getMaxLength() < parameter.minLength
                    || ra.getMinLength() > parameter.maxLength
                ) {
                    return false;
                }
                
            } else if (argument instanceof ImmediateArgument) {
            // INV: immediate
                if (parameter.type != Variable.Type.IMMEDIATE) {
                    return false;
                }
                ImmediateArgument ia = (ImmediateArgument) argument;
                if (ia.getMinLength() > parameter.maxLength) {
                    return false;
                }
            
            } else if (argument instanceof StringArgument) {
            // INV: string
                if (parameter.type != Variable.Type.STRING) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Conforms the invocation arguments to the invoked command. Must be called
     * after invokedCommand has been determined.
     * 
     * @param ast
     * @param implementation The implementation containing this invocation. Null
     *                       if within userland code.
     */
    @SuppressWarnings("incomplete-switch")
    private void conformArgs(AST ast, Implementation implementation)
    {
        Iterator<Variable> iter = this.invokedCommand.getParameters().iterator();
        for (Argument argument : this.arguments) {
            Variable parameter = iter.next();
            
            if (argument instanceof RawArgument) {
            // INV: register/var/label
                RawArgument ra = (RawArgument) argument;
                int index = this.arguments.indexOf(ra);
                switch (parameter.type) {
                    case REGISTER:
                    case STRING:
                    // CMD: register/var/string
                        this.arguments.set(
                            index,
                            new RegisterArgument(ra.potentialRegister)
                        );
                        break;

                    // TODO support label
                    //      - don't forget that the label name may have to
                    //        be resolved later (because the label may be
                    //        defined later in source code), but here we can
                    //        already create some LabelArgument if no
                    //        Variable with the used name exists.
                }
            }
        }
    }
    
    @Override
    public String toString()
    {
        if (this.isResolved()) {
            return this.invokedCommand + this.arguments.toString();
        }
        return this.name + " " + this.readableSignature;
    }
}
