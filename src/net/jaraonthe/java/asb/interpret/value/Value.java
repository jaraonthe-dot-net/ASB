package net.jaraonthe.java.asb.interpret.value;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.invocation.Argument;
import net.jaraonthe.java.asb.ast.invocation.ImmediateArgument;
import net.jaraonthe.java.asb.ast.invocation.LabelArgument;
import net.jaraonthe.java.asb.ast.invocation.RegisterArgument;
import net.jaraonthe.java.asb.ast.invocation.StringArgument;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;

/**
 * Contains the Value stored in a register, parameter, or local variable for use
 * in the interpretation phase.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Value
{
    /**
     * The Variable which this Value is assigned to.
     */
    public final VariableLike variable;

    /**
     * @param variable The Variable which this Value is assigned to
     */
    public Value(VariableLike variable)
    {
        this.variable = variable;
    }

    
    /**
     * Creates a Value instance from the given invocation argument.
     * 
     * @param argument  The invocation argument
     * @param parameter The invoked {@link Command}'s parameter that the
     *                  argument is used for
     * @param context   The context of the invocation (i.e. NOT of the invoked
     *                  command's interpretable; i.e. all the variables that
     *                  are visible at the invocation location are visible in
     *                  this context)
     * 
     * @return
     * 
     * @throws RuntimeError
     */
    public static Value fromArgument(Argument argument, Variable parameter, Context context) throws RuntimeError
    {
        switch (argument.getVariableType()) {
            case REGISTER:
                RegisterArgument ra = (RegisterArgument) argument;
                if (ra.hasPosition()) {
                    return new BitwiseNumericValue(
                        parameter,
                        context.frame.getNumericValue(ra.register.name),
                        ra.getEffectiveFromPosition(context),
                        ra.getEffectiveToPosition(context)
                    );
                }
                return new NumericValueReference(
                    parameter,
                    context.frame.getNumericValue(ra.register.name)
                );
                
            case IMMEDIATE:
                return new NumericValueStore(
                    parameter,
                    (ImmediateArgument) argument
                );
                
            case LABEL:
                if (parameter.localLabel) {
                    return new LabelValue(
                        parameter,
                        (LabelArgument) argument,
                        context.frame
                    );
                } else {
                    NumericValueStore value = new NumericValueStore(parameter, context.ast.getPcLength());
                    value.write(BigInteger.valueOf(((LabelArgument)argument).getLabelPosition()), context);
                    return value;
                }
                
            case STRING:
                return new StringValue(
                    parameter,
                    (StringArgument) argument
                );
                
            default:
                throw new IllegalArgumentException("Cannot use Local Variable as argument");
        }
    }
}
