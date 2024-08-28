package net.jaraonthe.java.asb.built_in;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.interpret.Interpretable;
import net.jaraonthe.java.asb.interpret.value.NumericValue;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

/**
 * The {@code &get_program_counter} (aka {@code &get_pc}) and
 * {@code &set_program_counter} (aka {@code &set_pc}) built-in functions.<br>
 * 
 * {@code &get_program_counter dstRegister};<br>
 * {@code &get_pc              dstRegister};<br>
 * {@code &set_program_counter srcRegister};<br>
 * {@code &set_pc              srcRegister};
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class ProgramCounter implements Interpretable
{
    public enum Type
    {
        GET_PROGRAM_COUNTER("&get_program_counter"),
        GET_PC             ("&get_pc"),
        SET_PROGRAM_COUNTER("&set_program_counter"),
        SET_PC             ("&set_pc");
        
        public final String functionName;
        
        private Type(String functionName)
        {
            this.functionName = functionName;
        }
    }
    
    protected final ProgramCounter.Type type;
    
    
    /**
     * @param type Selects the actual function
     */
    private ProgramCounter(ProgramCounter.Type type)
    {
        this.type = type;
    }

    /**
     * Creates a {@code &get_program_counter}, {@code &get_pc},
     * {@code &set_program_counter}, or {@code &set_pc} built-in function.
     * 
     * @param type Selects the actual function
     * 
     * @return
     */
    public static BuiltInFunction create(ProgramCounter.Type type)
    {
        BuiltInFunction function = new BuiltInFunction(type.functionName, false);

        function.addParameterByType(Variable.Type.REGISTER, "register");
        
        function.setInterpretable(new ProgramCounter(type));
        return function;
    }
    
    
    @Override
    public void interpret(Context context) throws RuntimeError
    {
        NumericValue register = context.frame.getNumericValue("register");
        
        if (register.length != context.ast.getPcLength()) {
            throw new RuntimeError(
                "Cannot use register " + register.getReferencedName() + " with "
                + this.type.functionName + " as it doesn't have the same length as the program counter"
            );
        }
        
        switch (this.type) {
            case GET_PROGRAM_COUNTER:
            case GET_PC:
                register.write(
                    BigInteger.valueOf(
                        // Subtracting 1 because pc is incremented BEFORE
                        // executing an invocation
                        context.frame.getRootParentFrame().programCounter - 1
                    ),
                    context
                );
                break;
            case SET_PROGRAM_COUNTER:
            case SET_PC:
                BigInteger value = NumericValueStore.normalizeBigInteger(
                    register.read(context),
                    context.ast.getPcLength()
                );
                int newPc;
                try {
                    newPc = value.intValueExact();
                } catch (ArithmeticException e) {
                    throw new RuntimeError(
                        "New program counter value exceeds technical limit, is " + value
                    );
                }
                
                // TODO Note in docs: Setting pc to a value that doesn't point
                //      to an actual command invocation, the program halts
                //      (same for jump on a local level)
                context.frame.getRootParentFrame().programCounter = newPc;
                break;
        }
    }
}
