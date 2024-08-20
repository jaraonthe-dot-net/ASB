package net.jaraonthe.java.asb.interpret;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.value.NumericValueReference;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;
import net.jaraonthe.java.asb.parse.Parser;

/**
 * Interpretes an AST that the {@link Parser} provided.
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
        this.initGlobalFrame();
    }

    /**
     * Runs this interpreter.
     * 
     * @throws RuntimeError
     */
    private void run() throws RuntimeError
    {
        Context context = new Context(this.globalFrame, this.ast);
        
        int i = 0;
        for (Invocation invocation : this.ast.getProgram()) {
            // TODO a more sophisticated program run output
            System.out.println(String.format("%4x: %s", i, invocation.toString()));
            invocation.interpret(context);
            
            i++;
        }
    }
    
    /**
     * Initializes the global frame.
     */
    private void initGlobalFrame() {
        for (Register register : this.ast.getRegisters()) {
            if (register instanceof RegisterAlias) {
                continue;
            }
            // TODO VirtualRegister
            this.globalFrame.addValue(new NumericValueStore(register));
        }
        
        // RegisterAlias
        // - this is done in an extra pass because the referenced register must
        //   already exist
        // TODO Double-check in the Parser that virtual registers can only refer
        //      to real registers in the language syntax - otherwise we need
        //      to do something more complex here.
        //      
        for (Register register : this.ast.getRegisters()) {
            if (!(register instanceof RegisterAlias)) {
                continue;
            }
            RegisterAlias ra = (RegisterAlias) register;
            this.globalFrame.addValue(new NumericValueReference(
                register,
                this.globalFrame.getNumericValue(ra.aliasedRegister.name)
            ));
        }
    }
}
