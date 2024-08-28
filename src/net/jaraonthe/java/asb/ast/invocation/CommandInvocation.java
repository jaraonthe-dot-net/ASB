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
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Frame;
import net.jaraonthe.java.asb.interpret.value.Value;

/**
 * An Invocation of a command.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class CommandInvocation extends CommandLike implements Invocation
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
    
    private boolean isResolved = false;
    
    
    /**
     * @param name of the invoked command
     */
    public CommandInvocation(String name)
    {
        super(name);
    }
    
    
    @Override
    public boolean isResolved()
    {
        return this.isResolved;
    }
    
    /**
     * @return The command that's being invoked. Null if it hasn't been resolved
     *         yet.
     */
    public Command getInvokedCommand()
    {
        return this.invokedCommand;
    }
    
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
    @Override
    public CommandInvocation resolve(AST ast, Implementation implementation) throws ConstraintException
    {
        if (this.invokedCommand != null) {
            throw new IllegalStateException(
                "Attempting to resolve an Invocation that has already been resolved"
            );
        }
        
        String resolvingClass = this.getResolvingClass();
        Set<Command> commandClass = ast.getCommandClass(resolvingClass);
        if (
            (commandClass == null || commandClass.isEmpty())
            && resolvingClass.contains(String.valueOf(Variable.Type.IMMEDIATE.signatureMarker))
        ) {
            // command with immediate parameter not found, let's try again with label
            resolvingClass = resolvingClass.replace(
                Variable.Type.IMMEDIATE.signatureMarker,
                Variable.Type.LABEL.signatureMarker
            );
            commandClass = ast.getCommandClass(resolvingClass);
        }
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
                        
                    case LABEL:
                        // That's fine
                        break;
                    
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
            // INV: immediate/label
                if (parameter.type == Variable.Type.LABEL) {
                    // That's fine
                    break;
                }
                if (parameter.type != Variable.Type.IMMEDIATE) {
                    return false;
                }
                ImmediateArgument ia = (ImmediateArgument) argument;
                if (ia.getMinLength() > parameter.maxLength) {
                    return false;
                }
            
            } else if (argument instanceof LabelArgument) {
            // INV: label
                if (parameter.type != Variable.Type.LABEL) {
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
                        
                    case LABEL:
                        this.arguments.set(index, new LabelArgument(ra.name));
                        break;
                }
            } else if (
                argument instanceof ImmediateArgument
                && parameter.type == Variable.Type.LABEL
            ) {
                ImmediateArgument ia = (ImmediateArgument) argument;
                int index = this.arguments.indexOf(ia);
                this.arguments.set(index, new LabelArgument(ia.asString));
            }
        }
    }
    

    @Override
    public Invocation resolveLabelNames(AST ast, Implementation implementation) throws ConstraintException
    {
        if (this.invokedCommand == null) {
            throw new IllegalStateException(
                "Cannot resolve labels when invoked command isn't resolved yet"
            );
        }
        
        for (Argument argument : this.arguments) {
            if (!(argument instanceof LabelArgument)) {
                continue;
            }
            LabelArgument la = (LabelArgument) argument;
            if (la.hasLabelPosition()) {
                break;
            }
            
            int labelPosition;
            boolean localLabel = this.invokedCommand.getParameterAt(this.arguments.indexOf(la)).localLabel;
            if (localLabel && implementation != null) {
                labelPosition = implementation.getLabel(la.name);
            } else {
                labelPosition = ast.getLabel(la.name);
            }
            if (labelPosition == -1) {
                throw new ConstraintException("Label " + la.name + " does not exist");
            }
            la.setLabelPosition(labelPosition);
        }
        
        this.isResolved = true;
        return this;
    }
    
    
    @Override
    public String toString()
    {
        if (this.invokedCommand != null) {
            return this.invokedCommand + this.arguments.toString();
        }
        return this.name + " " + this.readableSignature;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        // The frame for the command's interpretable must be constructed here,
        // as it has to be populated with the argument values
        Frame newFrame = new Frame(context.frame.getRootParentFrame());
        
        int i = 0;
        for (Argument argument : this.arguments) {
            newFrame.addValue(Value.fromArgument(
                argument,
                this.invokedCommand.getParameterAt(i),
                context
            ));
            i++;
        }
        
        this.invokedCommand.getInterpretable().interpret(context.withFrame(newFrame));
    }
}
