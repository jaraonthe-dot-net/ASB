package net.jaraonthe.java.asb.interpret;

import java.util.ArrayDeque;
import java.util.List;
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
            
            // This may modify the program Counter
            invocation.interpret(context);
        }
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
        if (this.settings.getDevMode() || this.settings.getTrace()) {
            if (this.settings.printOccurred) {
                System.out.println();
                this.settings.printOccurred = false;
            }
            
            String text;
            if (this.settings.getDevMode()) {
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
}
