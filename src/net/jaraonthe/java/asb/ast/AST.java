package net.jaraonthe.java.asb.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * This is the result of parsing ASB source code. Contains the entire userland
 * program as well as commands, (built-in) functions, and system properties.<br>
 * 
 * Note: This is the basis for interpreting the userland program, but is itself
 * not modified during interpretation (all dynamic data is stored elsewhere).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class AST
{
    private static final int DEFAULT_PC_LENGTH = 64;
    
    /* SYSTEM PROPERTIES */
    private int memoryWordLength    = -1;
    private int memoryAddressLength = -1;
    private int pcLength            = AST.DEFAULT_PC_LENGTH;
    
    /**
     * Calculated from pcLength. States how many items program can have at most.
     */
    private int maxProgramSize;
    
    /**
     * All (globally available) registers, accessed via their name.
     */
    private Map<String, Register> registers = HashMap.newHashMap(16);
    
    /**
     * All commands and (built-in) functions, accessed via their identity.
     */
    // TODO maybe we don't require both command maps?
    private Map<String, Command> commands = HashMap.newHashMap(32);
    
    /**
     * All commands and (built-in) functions, grouped by resolving class.
     */
    private Map<String, Set<Command>> commandsResolvingMap = HashMap.newHashMap(16);
    // TODO what about built-in functions? Load them into the commands register
    //      at start-up, or have some sort of lazy-load layer (maybe for the
    //      implementations)?
    
    /**
     * The actual userland program.
     */
    // TODO as this uses ints for the index, the size of a program is effectively
    //      limited regardless of pc length - note in the docs accordingly.
    //      (size limited to 2^32 - 1 == Integer.MAX_VALUE)
    private List<Invocation> program = new ArrayList<>(50);
    
    
    public AST()
    {
        this.calculateMaxProgramSize();
    }
    
    /**
     * Sets memory configuration.
     * 
     * @param wordLength    The length of one memory word
     * @param addressLength The length of a memory address
     * 
     * @return Fluent interface
     */
    public AST setMemory(int wordLength, int addressLength)
    {
        if (!Constraints.isValidLength(wordLength) || ! Constraints.isValidLength(addressLength)) {
            throw new IllegalArgumentException(
                "Invalid lengths for memory configuration. Given values are: wordLength: "
                + wordLength + ", addressLength: " + addressLength
            );
        }
        
        this.memoryWordLength = wordLength;
        this.memoryAddressLength = addressLength;
        
        return this;
    }

    /**
     * @return True if the system has memory configured.
     */
    public boolean hasMemory()
    {
        return this.memoryWordLength > 0;
    }
    
    /**
     * @return The configured memory wordLength. -1 if not configured yet.
     */
    public int getMemoryWordLength()
    {
        return this.memoryWordLength;
    }
    
    /**
     * @return The configured memory addressLength. -1 if not configured yet.
     */
    public int getMemoryAddressLength()
    {
        return this.memoryAddressLength;
    }
    
    /**
     * Sets program counter length.
     * 
     * @param length
     * @return Fluent interface
     */
    public AST setPcLength(int length)
    {
        if (!Constraints.isValidLength(length)) {
            throw new IllegalArgumentException(
                "Invalid program counter length. Given value is: " + length
            );
        }
        
        this.pcLength = length;
        this.calculateMaxProgramSize();
        
        return this;
    }
    
    /**
     * (Re-)Calculates maxProgramSize after pcLength has been changed.
     */
    private void calculateMaxProgramSize()
    {
        if (this.pcLength >= 31) {
            this.maxProgramSize = Integer.MAX_VALUE;
        }
        this.maxProgramSize = (int)Math.pow(2, this.pcLength);
    }
    
    /**
     * @return Configured program counter length.
     */
    public int getPcLength()
    {
        return this.pcLength;
    }
    
    
    /**
     * Adds a register.
     * 
     * @param register
     * @return Fluent interface
     */
    public AST addRegister(Register register)
    {
        this.registers.put(register.name, register);
        return this;
    }
    
    /**
     * @param registerName
     * @return True if a register with this name exists.
     */
    public boolean registerExists(String registerName)
    {
        return this.registers.containsKey(registerName);
    }
    
    /**
     * @param registerName
     * @return Register with the given name, or null if it doesn't exist
     */
    public Register getRegister(String registerName)
    {
        return this.registers.get(registerName);
    }
    
    
    /**
     * Adds a command.
     * 
     * @param command
     * @return Fluent interface
     */
    public AST addCommand(Command command)
    {
        this.commands.put(command.getIdentity(), command);
        
        String resolvingClass = command.getResolvingClass();
        if (!this.commandsResolvingMap.containsKey(resolvingClass)) {
            this.commandsResolvingMap.put(resolvingClass, HashSet.newHashSet(2));
        }
        this.commandsResolvingMap.get(resolvingClass).add(command);
        
        return this;
    }
    
    /**
     * @param commandIdentity see {@link Command#getIdentity()}
     * @return True if a command with the given identity exists.
     */
    public boolean commandExists(String commandIdentity)
    {
        return this.commands.containsKey(commandIdentity);
    }
    
    /**
     * @param commandIdentity see {@link Command#getIdentity()}
     * @return Command with the given identity, or null if it doesn't exist
     */
    public Command getCommand(String commandIdentity)
    {
        return this.commands.get(commandIdentity);
    }
    
    /**
     * @param resolvingClass see {@link Command#getResolvingClass()}
     * @return True if at least one command with the given resolving class exists.
     */
    public boolean commandClassExists(String resolvingClass)
    {
        return this.commandsResolvingMap.containsKey(resolvingClass);
    }
    
    /**
     * @param resolvingClass see {@link Command#getResolvingClass()}
     * @return All commands with the given resolving class (readonly set). May
     *         be null if resolvingClass does not exist yet.
     */
    public Set<Command> getCommandClass(String resolvingClass)
    {
        if (!this.commandsResolvingMap.containsKey(resolvingClass)) {
            return null;
        }
        return Collections.unmodifiableSet(this.commandsResolvingMap.get(resolvingClass));
    }
    
    /**
     * @return Readonly list of commands. This can be used to modify individual
     *         commands, but not the list of commands itself.
     */
    public Collection<Command> getCommands()
    {
        return Collections.unmodifiableCollection(this.commands.values());
    }
    
    
    /**
     * Adds an invocation to the userland program.
     * 
     * @param item
     * @return Fluent interface
     * @throws ConstraintException if adding this invocation would exceed
     *                             maximum allowed program size
     * 
     */
    public AST addToProgram(Invocation item) throws ConstraintException
    {
        if (this.program.size() >= this.maxProgramSize) {
            throw new ConstraintException(
                "Cannot have more than " + this.maxProgramSize
                + " machine commands due to program counter size being " + this.pcLength + " bits"
            );
        }
        this.program.add(item);
        return this;
    }
    
    /**
     * @return true if this contains at least one command invocation, i.e the
     *         userland program is not empty.
     */
    public boolean hasProgram()
    {
        return !this.program.isEmpty();
    }
}
