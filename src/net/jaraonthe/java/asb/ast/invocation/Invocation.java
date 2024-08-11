package net.jaraonthe.java.asb.ast.invocation;

import java.util.ArrayList;
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
import net.jaraonthe.java.asb.parse.Token;

/**
 * An Invocation of a command. This is what makes up the userland program.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Invocation extends CommandLike
{
    /**
     * Raw argument data, which is used to resolve invokedCommand and
     * generate actual arguments.
     */
    // TODO arg may be a var reference + bit pos (within an implementation) - how to model that?
    private List<Token> rawArguments = new ArrayList<>(3);
    
    /**
     * Once the referenced command has been resolved ({@link #resolve()}), this
     * points to the command that's being invoked. Otherwise this is null.
     */
    private Command invokedCommand = null;
    
    /**
     * Once the referenced command has been resolved ({@link #resolve()}), this
     * contains the actual arguments (which will be used by the interpreter).
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
        this.addCommandSymbolsToResolvingSignature(symbols);
    }
    
    /**
     * Adds an argument token to this invocation.<br>
     * 
     * Together with {@link #addCommandSymbols()}, this must be invoked in the
     * order in which these Tokens appear in the ASB source code.
     * 
     * @param argumentToken The Token representing a raw argument.
     */
    public void addArgument(Token argumentToken)
    {
        Variable.Type type;
        switch (argumentToken.type) {
            case NAME:
            case LABEL_NAME:
                type = Variable.Type.REGISTER;
                break;
            case NUMBER:
                type = Variable.Type.IMMEDIATE;
                break;
            case STRING:
                type = Variable.Type.STRING;
                break;
            default:
                throw new IllegalArgumentException("Unexpected Token Type " + argumentToken.type);
        }
        this.addParameterToResolvingSignature(type);
        this.rawArguments.add(argumentToken);
    }
    
    /**
     * Resolves this invocation. I.e. figures out which command is being invoked
     * and what the actual arguments are.
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
            // TODO include: some sort of readable invocation signature
            throw new ConstraintException("No command found for Invocation " + this.name);
        }
        
        // viable commands (all these fit the invocation signature & arguments)
        List<Command> viableCommands = new ArrayList<>(commandClass.size());
        for (Command c : commandClass) {
            if (this.isCommandViable(c, ast, implementation)) {
                viableCommands.add(c);
            }
        }
        if (viableCommands.isEmpty()) {
            // TODO include: some sort of readable invocation signature
            // TODO this error could be due to using registers/vars that don't
            //      exist - can that be detected somehow, and a more appropriate
            //      error message be given in that case? Or at least point out
            //      this potential root cause in the existing message.
            throw new ConstraintException("No command found for Invocation " + this.name);
        }
        
        if (viableCommands.size() > 1) {
            // select best command
            // TODO choose a command variant (apply tie-breakers via sorting)
            throw new RuntimeException(
                "Resolving against multiple viable Command Variants not implemented yet"
            );
        }
        
        // TODO somehow(?) ensure that only those local variables that have been
        //      defined before this invocation are used as arguments
        
        this.invokedCommand = viableCommands.getFirst();
        this.createActualArgs(ast, implementation);
        
        if (implementation == null && !this.invokedCommand.isUserlandInvokable()) {
            throw new ConstraintException(
                // TODO include: readable invocation signature
                "Attempting to invoke Function " + this.name
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
        for (Token argument : this.rawArguments) {
            Variable parameter = iter.next();
            
            switch (argument.type) {
                case NAME:
                case LABEL_NAME:
                // INV: register/var/label
                    switch (parameter.type) {
                        case REGISTER:
                        // CMD: register/var
                            int length;
                            if (implementation != null && implementation.variableExists(argument.content)) {
                                
                                // INV: local variable/param
                                Variable referenced = implementation.getVariable(argument.content);
                                if (!referenced.type.hasLength) {
                                    // i.e. this is a label or string parameter
                                    return false;
                                }
                                length = referenced.length;
                                if (
                                    parameter.hasGroup()
                                    && !parameter.getGroup().equals(referenced.getGroup())
                                ) {
                                    // group doesn't fit
                                    return false;
                                }
                            } else if (ast.registerExists(argument.content)) {
                                
                                // INV: global register 
                                Register referenced = ast.getRegister(argument.content);
                                length = referenced.length;
                                if (
                                    parameter.hasGroup()
                                    && !referenced.hasGroup(parameter.getGroup())
                                ) {
                                    // group doesn't fit any of register
                                    return false;
                                }
                            } else {
                                // no var found for argument name
                                return false;
                            }
                            if (length != parameter.length) {
                                // TODO support more advanced length matchers
                                return false;
                            }
                            break;

                        // TODO support label
                        
                        case STRING:
                        // CMD: string
                            if (
                                implementation == null
                                || !implementation.variableExists(argument.content)
                                || implementation.getVariable(argument.content).type != Variable.Type.STRING
                            ) {
                                // no local var found or incorrect type
                                return false;
                            }
                            break;
                            
                        default:
                            return false;
                    }
                    break;
                    
                case NUMBER:
                // INV: immediate
                    if (parameter.type != Variable.Type.IMMEDIATE) {
                        return false;
                    }
                    // TODO check length
                    break;
                    
                case STRING:
                // INV: string
                    if (parameter.type != Variable.Type.STRING) {
                        return false;
                    }
                    break;
            }
        }
        
        return true;
    }
    
    /**
     * Creates actual arguments from rawArguments. Must be called after
     * invokedCommand has been determined.
     * 
     * @param ast
     * @param implementation The implementation containing this invocation. Null
     *                       if within userland code.
     */
    private void createActualArgs(AST ast, Implementation implementation)
    {
        Iterator<Variable> iter = this.invokedCommand.getParameters().iterator();
        for (Token argument : this.rawArguments) {
            Variable parameter = iter.next();
            
            switch (argument.type) {
                case NAME:
                case LABEL_NAME:
                // INV: register/var/label
                    switch (parameter.type) {
                        case REGISTER:
                        case STRING:
                        // CMD: register/var/string
                            int length;
                            if (implementation != null && implementation.variableExists(argument.content)) {
                                // INV: local variable/param
                                this.arguments.add(new RegisterArgument(
                                    implementation.getVariable(argument.content)
                                ));
                            } else {
                                // INV: global register
                                this.arguments.add(new RegisterArgument(
                                    ast.getRegister(argument.content)
                                ));
                            }
                            break;

                        // TODO support label
                    }
                    break;
                    
                case NUMBER:
                // immediate
                    this.arguments.add(new ImmediateArgument(Token.number2BigInteger(argument)));
                    break;
                    
                case STRING:
                // string
                    this.arguments.add(new StringArgument(argument.content));
                    break;
            }
        }
    }
}
