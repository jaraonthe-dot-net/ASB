package net.jaraonthe.java.asb.interpret;

import java.util.ArrayDeque;
import java.util.Queue;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.ast.variable.VirtualRegister;
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
     * The global frame containing register values.
     */
    private Frame globalFrame = new Frame();
    
    /**
     * The memory of the virtual system. May be null (if no memory is configured).
     */
    private Memory memory = null;
    
    
    /**
     * Executes the entire interpreting procedure; i.e. interpreting the
     * userland program.
     * 
     * @param ast the AST that shall be interpreted
     * @throws RuntimeError
     */
    public static void interpret(AST ast) throws RuntimeError
    {
        new Interpreter(ast).run();
    }
    
    
    /**
     * @param ast the AST that shall be interpreted by this Interpreter
     */
    protected Interpreter(AST ast)
    {
        this.ast = ast;
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
        Context context = new Context(this.globalFrame, this.memory, this.ast);
        
        int i = 0;
        for (Invocation invocation : this.ast.getProgram()) {
            // TODO a more sophisticated program run output
            System.err.println(String.format("%4x: %s", i, invocation.toString()));
            invocation.interpret(context);
            
            i++;
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
            } catch (RuntimeError e) {
                // Converting exception, as this case should never happen
                throw new RuntimeException(e);
            }
        }
    }
}
