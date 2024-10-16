package net.jaraonthe.java.asb.ast.invocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.CommandLike;
import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Frame;
import net.jaraonthe.java.asb.interpret.value.Value;
import net.jaraonthe.java.asb.parse.Origin;

/**
 * An Invocation of a command.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class CommandInvocation extends CommandLike implements Invocation, Comparator<Command>
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
    private Origin origin;
    
    
    /**
     * @param name of the invoked command
     */
    public CommandInvocation(String name)
    {
        super(name);
    }

    
    @Override
    public Origin getOrigin()
    {
        return this.origin;
    }
    
    /**
     * Sets this invocation's origin.
     * 
     * @param origin
     * @return Fluent interface
     */
    public CommandInvocation setOrigin(Origin origin)
    {
        this.origin = origin;
        return this;
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
        /*
         * Resolving invocation to invoked command procedure:
         * - Command symbols must fit
         * - Invocation arguments must fit command parameters:
         *   Here we have some ambiguity, so it is more complex. There may be
         *   more than one command that fits the arguments, in which case tie-
         *   breakers are applied to decide which one is picked.
         *   
         * Arguments => Parameters:
         * - ImmediateArgument => IMMEDIATE; if no command fits => LABEL
         *   (this ignores any tie-breaker order, the IMMEDIATE is always
         *   preferred if possible)
         * - RegisterArgument  => REGISTER
         * - LabelArgument     => LABEL
         * - RawArgument       => REGISTER, LABEL
         * - StringArgument    => STRING
         * 
         * Parameter constraints that must be satisfied:
         * - IMMEDIATE: immediate length <= parameter length
         * - REGISTER:
         *   variable referenced in argument must exist; referenced variable
         *   must be numeric; if parameter has group, register used in argument
         *   must have same group; argument's length range must overlap with
         *   parameter's length range (consider dynamic length -
         *   RegisterArgument has a complex calculation for its length range
         *   that is used here)
         * 
         * Tie-breakers are applied left to right to parameters; first parameter
         * that is different between potential commands decides.
         * The various tie-breakers (for each type applied in order as given):
         * - ImmediateArgument (IMMEDIATE):
         *   => select command with param with smaller length
         *      (Rationale: It's probably more efficient to use this command
         *      than one that can handle longer immediates)
         * - RegisterArgument:
         *   => prefer group over non-group param;
         *   => if both have group: Prefer the one of which the group comes
         *      first in the register's group list;
         *   => as we have dynamic length we don't know which command to pick
         *      and we move on to the next argument (this may lead to an error
         *      being triggered)
         *      (this may be changed in the future; it requires the invoked
         *      command to be determined at runtime (which makes everything more
         *      complex and I'd rather avoid that - I think it is possible to
         *      work your way around this limitation when implementing commands)
         * - RawArgument:
         *   => prefer REGISTER over LABEL;
         *   => if REGISTER: apply tie-breakers for RegisterArgument
         *   
         */
        
        if (this.invokedCommand != null) {
            throw new IllegalStateException(
                "Attempting to resolve an Invocation that has already been resolved"
            );
        }
        
        String resolvingClass = this.getResolvingClass();
        List<Command> viableCommands;
        int tryAgain = 0;
        if (resolvingClass.contains(String.valueOf(Variable.Type.IMMEDIATE.signatureMarker))) {
            tryAgain = 1;
        }
        do {
            Set<Command> commandClass = ast.getCommandClass(resolvingClass);
            if (commandClass == null) {
                commandClass = HashSet.newHashSet(0);
            }
            
            // viable commands (all these fit the invocation signature & arguments)
            viableCommands = new ArrayList<>(commandClass.size());
            for (Command c : commandClass) {
                if (this.isCommandViable(c, ast, implementation)) {
                    viableCommands.add(c);
                }
            }
            if (viableCommands.isEmpty()) {
                if (tryAgain > 0) {
                    // command with immediate parameter not found, let's try again with label
                    resolvingClass = resolvingClass.replace(
                        Variable.Type.IMMEDIATE.signatureMarker,
                        Variable.Type.LABEL.signatureMarker
                    );
                    continue;
                }
                
                // TODO Consider improving error message, as this error could be
                //      due to using registers/vars that don't exist - can that
                //      be detected somehow, and a more appropriate error
                //      message be given in that case? For now, we just point
                //      out this potential root cause in the message.
                throw new ConstraintException(
                    "No command found for Invocation " + this + " - maybe used registers don't exist?"
                );
            }
            break;
        } while (tryAgain-- > 0);
        
        if (viableCommands.size() > 1) {
            // select best command
            viableCommands.sort(this);
            
            if (this.compare(viableCommands.get(0), viableCommands.get(1)) == 0) {
                throw new ConstraintException(
                    "Cannot determine which Command to invoke by " + this
                    + ". Most likely this is due to dynamic length (or dynamic position) in"
                    + " combination with several command variants that only differ by a parameter's"
                    + " length; it is impossible to make a decision at compile time."
                    + " Try to rewrite your program"
                );
            }
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
                            // Just to be safe
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

    @Override
    public int compare(Command a, Command b)
    {
        // Applying tie-breakers (see note in resolve())
        Iterator<Variable> iterA = a.getParameters().iterator();
        Iterator<Variable> iterB = b.getParameters().iterator();
        for (Argument argument : this.arguments) {
            Variable paramA = iterA.next();
            Variable paramB = iterB.next();
            
            if (argument instanceof LabelArgument || argument instanceof StringArgument) {
                continue;
            }
            
            if (argument instanceof ImmediateArgument) {
                if (paramA.type == Variable.Type.LABEL) {
                    // both candidates are expected to have the same type
                    continue;
                }
                
                // prefer smaller length
                if (paramA.maxLength < paramB.maxLength) {
                    return -1;
                } else if (paramA.maxLength > paramB.maxLength) {
                    return 1;
                }
                continue;
            }
            
            if (argument instanceof RawArgument) {
                if (paramA.type != paramB.type) {
                    // prefer REGISTER over LABEL
                    return paramA.type == Variable.Type.REGISTER ? -1 : 1;
                }
                if (paramA.type == Variable.Type.LABEL) {
                    // both candidates have same type
                    continue;
                }
            }
            
            // REGISTER parameters
            
            if (paramA.hasGroup() != paramB.hasGroup()) {
                // prefer group over non-group
                return paramA.hasGroup() ? -1 : 1;
            }
            if (!paramA.hasGroup()) {
                // both have no group - this may mean we cannot decide after all
                continue;
            }

            // prefer first listed group
            Register referencedRegister;
            if (argument instanceof RawArgument) {
                referencedRegister = (Register) ((RawArgument)argument).potentialRegister;
            } else {
                referencedRegister = (Register) ((RegisterArgument)argument).register;
            }
            int posA = referencedRegister.getGroupPosition(paramA.getGroup());
            int posB = referencedRegister.getGroupPosition(paramB.getGroup());
            if (posA < posB) {
                return -1;
            }
            if (posA > posB) {
                return 1;
            }
        }
        
        return 0;
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
            try {
                newFrame.addValue(Value.fromArgument(
                    argument,
                    this.invokedCommand.getParameterAt(i),
                    context
                ));
            } catch (ConstraintException e) {
                throw new RuntimeError(e.getMessage() + " at " + this.getOrigin());
            }
            i++;
        }
        
        try {
            this.invokedCommand.getInterpretable().interpret(context.withFrame(newFrame));
        } catch (ConstraintException e) {
            throw new RuntimeError(e.getMessage() + " at " + this.getOrigin());
        }
    }
}
