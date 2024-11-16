package net.jaraonthe.java.asb.interpret;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

import net.jaraonthe.java.asb.Print;
import net.jaraonthe.java.asb.Settings;
import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.ast.variable.VirtualRegister;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueReference;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;
import net.jaraonthe.java.asb.interpret.value.VirtualNumericValue;
import net.jaraonthe.java.asb.parse.Parser;

/**
 * Interprets an AST that the {@link Parser} provided.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Interpreter
{
    /**
     * The AST that is interpreted by this Interpreter.
     */
    private final AST ast;

    /**
     * General program settings
     */
    private final Settings settings;
    
    /**
     * The global frame containing register values.
     */
    private Frame globalFrame = new Frame();
    
    /**
     * The memory of the virtual system. May be null (if no memory is configured).
     */
    private Memory memory = null;
    
    /**
     * Statistics for this interpreter run.
     */
    private Statistics statistics = new Statistics();
    
    
    private static final Pattern PRINT_STRING_PATTERN = Pattern.compile(" \"([^\"]|\\\")*\"");
    
    
    
    /**
     * Executes the entire interpreting procedure; i.e. interpreting the
     * userland program.
     * 
     * @param ast      The AST that shall be interpreted
     * @param settings General program settings
     * 
     * @throws RuntimeError
     */
    public static void interpret(AST ast, Settings settings) throws RuntimeError
    {
        new Interpreter(ast, settings).run();
    }
    
    
    /**
     * @param ast      The AST that shall be interpreted by this Interpreter
     * @param settings General program settings
     */
    protected Interpreter(AST ast, Settings settings)
    {
        this.ast      = ast;
        this.settings = settings;
        if (ast.hasMemory()) {
            this.memory = new Memory(ast.getMemoryWordLength(), ast.getMemoryAddressLength());
        }
        this.initGlobalFrame();
    }
    
    /**
     * Initializes the global frame.
     */
    private void initGlobalFrame() {
        Queue<RegisterAlias> registerAliases = new ArrayDeque<>();
        for (Register register : this.ast.getRegisters()) {
            if (register instanceof RegisterAlias) {
                registerAliases.add((RegisterAlias)register);
                continue;
            }
            if (register instanceof VirtualRegister) {
                this.globalFrame.addValue(new VirtualNumericValue((VirtualRegister)register));
                continue;
            }
            this.globalFrame.addValue(new NumericValueStore(register));
        }
        
        // RegisterAlias
        // - this is done in an extra pass because the referenced register must
        //   already exist
        while (!registerAliases.isEmpty()) {
            RegisterAlias ra = registerAliases.poll();
        
            if (!this.globalFrame.valueExists(ra.aliasedRegister.name)) {
                // The referenced register may be an alias as well, thus may not
                // exist yet - let's retry later
                registerAliases.add(ra);
                continue;
            }
            
            try {
                this.globalFrame.addValue(new NumericValueReference(
                    ra,
                    this.globalFrame.getNumericValue(ra.aliasedRegister.name)
                ));
            } catch (ConstraintException e) {
                // Converting exception, as this case should never happen
                throw new RuntimeException(e);
            }
        }
    }
    
    
    /**
     * Runs this interpreter.
     * 
     * @throws RuntimeError
     */
    private void run() throws RuntimeError
    {
        Context context = new Context(this.globalFrame, this.memory, this.ast, this.settings);
        
        List<Invocation> program = this.ast.getProgram();
        int programAddressLength = Math.max( // length in hex
            (int) Math.ceil(
                Math.log(program.size()) / Math.log(16)
            ),
            1
        );
        while (true) {
            int currentProgramCounter = this.globalFrame.programCounter;
            Invocation invocation;
            try {
                invocation = program.get(currentProgramCounter);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            // Incrementing pc before execution so that jumps can modify pc
            // without extra complexity
            this.globalFrame.programCounter++;

            this.printTrace(invocation, currentProgramCounter, programAddressLength);
            this.statistics.incrementInvocationsCount(invocation);
            
            // This may modify the program Counter
            try {
                invocation.interpret(context);
            } catch (StackOverflowError e) {
                throw new RuntimeError(
                    "Infinite recursion triggered by "
                    + (this.settings.devMode() ? invocation : invocation.getOrigin().getContent())
                    + " at " + invocation.getOrigin()
                );
            }
        }
        
        this.printStatistics();
        this.printRegisters(context);
        this.printMemory();
    }
    
    
    /**
     * Prints the current trace (if so configured).
     * 
     * @param invocation
     * @param currentProgramCounter
     * @param programAddressLength  How many chars to reserve for the pc value
     *                              output.
     */
    private void printTrace(Invocation invocation, int currentProgramCounter, int programAddressLength)
    {
        if (!this.settings.devMode() && !this.settings.trace()) {
            return;
        }
        
        this.printlnIfRequired();
        
        String text;
        if (this.settings.devMode()) {
            text = invocation.toString(); // incl. technical details
        } else {
            text = invocation.getOrigin().getContent(); // as written in ASB source code
            // Remove &print & &println string arguments, as they are
            // redundant information (they are printed on the next line)
            text = Interpreter.PRINT_STRING_PATTERN.matcher(text).replaceFirst(":");
        }
        
        Print.printlnWithColor(
            String.format(
                "    %" + programAddressLength + "x: %s",
                currentProgramCounter,
                text
            ),
            Print.Color.YELLOW,
            settings
        );
    }
    
    /**
     * Prints statistics (at the end of interpretation).
     */
    private void printStatistics()
    {
        if (!this.settings.statistics()) {
            return;
        }
        
        this.printlnIfRequired();
        System.out.println();
        Print.printlnBoldWithColor("=== STATISTICS ===", Print.Color.GREEN, this.settings);
        
        Map<String, Integer> invocationsCount = this.statistics.getInvocationsCount();
        // Sorted identities list
        String[] identities = invocationsCount.keySet().toArray(new String[invocationsCount.size()]);
        Arrays.sort(identities);
        
        // Readable identities
        String[] readableIdentities = new String[identities.length];
        int firstColLength = "Command ".length();
        for (int i = 0; i < identities.length; i++) {
            readableIdentities[i] = this.ast.getCommand(identities[i]).getReadableIdentity();
            firstColLength = Math.max(firstColLength, readableIdentities[i].length());
        }
        
        // Table Header
        Print.printlnWithColor(
            String.format(
                "%-" + firstColLength + "s\tExecuted",
                "Command"
            ),
            Print.Color.CYAN,
            settings
        );
        
        for (int i = 0; i < identities.length; i++) {
            System.out.format(
                "%-" + firstColLength + "s\t%d%n",
                readableIdentities[i],
                invocationsCount.get(identities[i])
            );
        }
    }
    
    /**
     * Prints register values (at the end of interpretation).
     * 
     * @param context
     * @throws RuntimeError may occur when reading from a {@link VirtualRegister}
     */
    private void printRegisters(Context context) throws RuntimeError
    {
        if (!this.settings.registers()) {
            return;
        }
        
        this.printlnIfRequired();
        System.out.println();
        Print.printlnBoldWithColor("=== REGISTER VALUES ===", Print.Color.BLUE, this.settings);
        
        // Sorted names list
        List<String> names = new ArrayList<String>();
        /**
         * register names => [alias names]
         */
        Map<String, List<String>> aliases = new HashMap<>();
        for (Register register : this.ast.getRegisters()) {
            if (register instanceof RegisterAlias) {
                continue;
            }
            names.add(register.name);
            aliases.put(register.name, new ArrayList<>());
        }
        if (names.isEmpty()) {
            return;
        }
        names.sort(null);
        
        // Aliases
        for (Register register : this.ast.getRegisters()) {
            if (!(register instanceof RegisterAlias)) {
                continue;
            }
            RegisterAlias ra = (RegisterAlias) register;
            aliases.get(ra.aliasedRegister.name).add(ra.name);
        }
        /**
         * register names => displayed name of that register (i.e. incl. alias names)
         */
        Map<String, String> displayedNamesMap = HashMap.newHashMap(names.size());
        int firstColLength = 0;
        for (String name : names) {
            List<String> associatedAliases = aliases.get(name);
            associatedAliases.sort(null);

            String aliasString = "";
            for (String alias : associatedAliases) {
                if (aliasString.isEmpty()) {
                    aliasString = alias;
                } else {
                    aliasString += ", " + alias;
                }
            }
            
            String displayedName = name;
            if (!aliasString.isEmpty()) {
                displayedName = name + " (" + aliasString + ")";
            }
            displayedNamesMap.put(name, displayedName);
            firstColLength = Math.max(firstColLength, displayedName.length());
        }
        
        for (String name : names) {
            try {
                NumericValue value = this.globalFrame.getNumericValue(name);
                BigInteger content = value.read(context);
                System.out.format(
                    "%-" + firstColLength + "s\t%d\t(0x%0" + Math.ceilDiv(value.length, 4) + "x)%n",
                    displayedNamesMap.get(name),
                    content,
                    content
                );
            } catch (ConstraintException e) {
                // Converting exception, as this case should never happen
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Prints memory values (at the end of interpretation). This displays only
     * those memory cells that have a value not equal to 0.
     */
    private void printMemory()
    {
        if (!this.settings.memory()) {
            return;
        }
        
        this.printlnIfRequired();
        System.out.println();
        Print.printlnBoldWithColor("=== MEMORY VALUES ===", Print.Color.MAGENTA, this.settings);
        
        if (this.memory == null) {
            System.out.println("(no memory configured)");
            return;
        }
        
        List<BigInteger> addresses = new ArrayList<>(this.memory.getAddressesInUse());
        addresses.sort(null);
        
        int addressLengthHex = Math.ceilDiv(this.ast.getMemoryAddressLength(), 4);
        int wordLengthHex    = Math.ceilDiv(this.ast.getMemoryWordLength(), 4);
        
        for (BigInteger address : addresses) {
            BigInteger value = this.memory.read(address);
            if (value.equals(BigInteger.ZERO)) {
                continue;
            }
            System.out.format(
                "0x%0" + addressLengthHex + "x\t%d\t(0x%0" + wordLengthHex + "x)%n",
                address,
                value,
                value
            );
        }
    }
    
    /**
     * Executes a {@code println()} if required (because a {@link
     * Settings#printOccurred printOccurred}). 
     */
    private void printlnIfRequired()
    {
        if (this.settings.printOccurred) {
            System.out.println();
            this.settings.printOccurred = false;
        }
    }
}
