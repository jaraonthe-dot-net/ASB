package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * An invocation argument that refers to a register or parameter or local
 * variable.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class VariableArgument extends Argument
{
    /**
     * The register or parameter or local variable this refers to
     */
    public final Variable variable;
    
    /**
     * Start position of bitwise access.<br>
     * -1 if bitwise access is not used or {@link #fromPositionVariable} is
     * used instead to dynamically supply this value.
     */
    public final int fromPosition;

    /**
     * End position of bitwise access.<br>
     * -1 if bitwise access is not used or {@link #toPositionVariable} is
     * used instead to dynamically supply this value.
     */
    public final int toPosition;
    
    /**
     * Variable used as start position of bitwise access.<br>
     * Null if bitwise access is not used or {@link #fromPosition} is
     * used as value instead.
     */
    public final Variable fromPositionVariable;

    /**
     * Variable used as end position of bitwise access.<br>
     * Null if bitwise access is not used or {@link #toPosition} is
     * used as value instead.
     */
    public final Variable toPositionVariable;
    

    /**
     * @param variable The register or parameter or local variable this argument
     *                 refers to
     */
    public VariableArgument(Variable variable)
    {
        this(variable, -1, -1, null, null);
    }

    /**
     * @param variable The register or parameter or local variable this argument
     *                 refers to
     * @param position The single bit position that is being accessed
     */
    public VariableArgument(Variable variable, int position)
    {
        this(variable, position, position, null, null);
    }

    /**
     * @param variable     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The start of the position range that is being accessed
     * @param toPosition   The end of the position range that is being accessed
     */
    public VariableArgument(Variable variable, int fromPosition, int toPosition)
    {
        this(variable, fromPosition, toPosition, null, null);
    }

    /**
     * @param variable The register or parameter or local variable this argument
     *                 refers to
     * @param position The variable which's value will be used as the single bit
     *                 position that is being accessed
     */
    public VariableArgument(Variable variable, Variable position)
    {
        this(variable, -1, -1, position, position);
    }

    /**
     * @param variable     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The variable which's value will be used as the start
     *                     of the position range that is being accessed
     * @param toPosition   The variable which's value will be used as the end
     *                     of the position range that is being accessed
     */
    public VariableArgument(Variable variable, Variable fromPosition, Variable toPosition)
    {
        this(variable, -1, -1, fromPosition, toPosition);
    }

    /**
     * @param variable     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The start of the position range that is being accessed
     * @param toPosition   The variable which's value will be used as the end
     *                     of the position range that is being accessed
     */
    public VariableArgument(Variable variable, int fromPosition, Variable toPosition)
    {
        this(variable, fromPosition, -1, null, toPosition);
    }

    /**
     * @param variable     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The variable which's value will be used as the start
     *                     of the position range that is being accessed
     * @param toPosition   The end of the position range that is being accessed
     */
    public VariableArgument(Variable variable, Variable fromPosition, int toPosition)
    {
        this(variable, -1, toPosition, fromPosition, null);
    }
    
    
    /**
     * @param variable
     * @param fromPosition
     * @param toPosition
     * @param fromPositionVariable
     * @param toPositionVariable
     */
    private VariableArgument(
        Variable variable,
        int fromPosition,
        int toPosition,
        Variable fromPositionVariable,
        Variable toPositionVariable
    ) {
        this.variable             = variable;
        this.fromPosition         = fromPosition;
        this.toPosition           = toPosition;
        this.fromPositionVariable = fromPositionVariable;
        this.toPositionVariable   = toPositionVariable;

        // Check arguments for consistency
        if (this.hasPosition() == (toPosition == -1 && toPositionVariable == null)) {
            throw new IllegalArgumentException(
                "Inconsistent position settings. Probably used -1 or null somewhere"
            );
        }
        
        if (this.hasPosition()) {
            if (!variable.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use bitwise access on non-numeric variable " + variable.name
                );
            }
            
            // from
            if (fromPosition != -1) {
                if (!Constraints.isValidPosition(fromPosition)) {
                    throw new IllegalArgumentException(
                        "Invalid argument " + variable.name + " start position. Given value: " + fromPosition
                    );
                }
                if (variable.maxLength > 0 && fromPosition >= variable.maxLength) {
                    throw new IllegalArgumentException(
                        "Argument " + variable.name + " start position is not within parameter length. Given value: " + fromPosition
                    );
                }
                
            } else if (!fromPositionVariable.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use non-numeric variable " + fromPositionVariable.name
                    + " as bitwise access position"
                );
            }
            
            // to
            if (toPosition != -1) {
                if (!Constraints.isValidPosition(toPosition)) {
                    throw new IllegalArgumentException(
                        "Invalid argument " + variable.name + " end position. Given value: " + toPosition
                    );
                }
                if (variable.maxLength > 0 && toPosition >= variable.maxLength) {
                    throw new IllegalArgumentException(
                        "Argument " + variable.name + " end position is not within parameter length. Given value: " + toPosition
                    );
                }
                
            } else if (!toPositionVariable.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use non-numeric variable " + toPositionVariable.name
                    + " as bitwise access position"
                );
            }
        }
    }
    
    
    /**
     * @see #hasDynamicPosition()
     * @return True if this argument has positional access settings
     */
    public boolean hasPosition()
    {
        return this.fromPosition != -1 || this.fromPositionVariable != null;
    }
    
    /**
     * @see #hasPosition()
     * @return True if this argument has positional access settings which are
     *         using dynamic position.
     */
    public boolean hasDynamicPosition()
    {
        return this.fromPositionVariable != null || this.toPositionVariable != null;
    }
    
    /**
     * @return The minimum length this argument may represent. Returns
     *         {@link Constraints.MIN_LENGTH MIN_LENGTH} if dynamic length
     *         or variable-based bitwise access is used. If the referenced
     *         Variable is non-numeric the return value is undefined.
     */
    public int getMinLength()
    {
        if (this.hasPosition()) {
            if (this.hasDynamicPosition()) {
                return Constraints.MIN_LENGTH;
            }
            return Math.abs(this.fromPosition - this.toPosition) + 1;
        }
        
        if (this.variable.minLength == 0) {
            // dynamic length
            return Constraints.MIN_LENGTH;
        }
        return this.variable.minLength;
    }
    
    /**
     * @return The maximum length this argument may represent. May returns
     *         {@link Constraints.MAX_LENGTH MAX_LENGTH} if dynamic length
     *         or variable-based bitwise access is used. If the referenced
     *         Variable is non-numeric the return value is undefined.
     */
    public int getMaxLength()
    {
        if (this.hasPosition()) {
            if (this.hasDynamicPosition()) {
                // assume both to and from are dynamic
                int position = 0;
                if (this.fromPositionVariable != null) {
                    if (this.toPositionVariable == null) {
                        // from is dynamic, to is fixed
                        position = this.toPosition;
                    }
                } else if (this.toPositionVariable == null) {
                    // to is dynamic, from is fixed
                    position = this.fromPosition;
                }
                return this.calculateMaxLength(position);
            }
            return Math.abs(this.fromPosition - this.toPosition) + 1;
        }
        
        return this.calculateMaxLength(0);
    }
    
    /**
     * Calculates maximum possible length when one length parameter is given
     * and the other is dynamic.
     * 
     * Even in such a situation a maximum length can be calculated because the
     * maximum length of the reference variable limits what is possible.
     * 
     * @param position
     * @return
     */
    private int calculateMaxLength(int position)
    {
        int length = this.variable.maxLength;
        if (this.variable.maxLength < 1) {
            length = Constraints.MAX_LENGTH;
        }
        if (position < length / 2) {
            // assume other position is the highest possible value
            return length - position;
        }
        // assume other position is 0
        return position + 1;
    }

    @Override
    public Parameter.Type getParameterType()
    {
        return Parameter.Type.REGISTER;
    }
    
    @Override
    public String toString()
    {
        if (!this.hasPosition()) {
            return this.variable.name;
        }
        
        String position = "";
        if (this.fromPosition != -1) {
            position += this.fromPosition;
            
            if (this.toPositionVariable != null) {
                position += ":" + this.toPositionVariable.name;
            } else if (this.toPosition != this.fromPosition) {
                position += ":" + this.toPosition;
            }
            
        } else {
            position += this.fromPositionVariable.name;
            
            if (this.toPosition != -1) {
                position += ":" + this.toPosition;
            } else if (this.toPositionVariable != this.fromPositionVariable) {
                position += ":" + this.toPositionVariable.name;
            }
        }
        
        return this.variable.name + "'" + position;
    }

    
    /**
     * Calculates the effective start position of bitwise access.
     * 
     * This also resolves dynamic position (via position variable) using the
     * given interpretation context.
     * 
     * @param context
     * 
     * @return
     * 
     * @throws ConstraintException
     * @throws RuntimeError
     * @throws IllegaleStateException if {@link #hasPosition()} is false
     */
    public int getEffectiveFromPosition(Context context) throws ConstraintException, RuntimeError
    {
        if (this.fromPosition != -1) {
            return this.fromPosition;
        }
        if (this.fromPositionVariable != null) {
            return this.variable2Position(this.fromPositionVariable, context);
        }
        
        throw new IllegalStateException(
            "Cannot compute effective fromPosition when not accessing Variable bitwise"
        );
    }
    
    /**
     * Calculates the effective end position of bitwise access.
     * 
     * This also resolves dynamic position (via position variable) using the
     * given interpretation context.
     * 
     * @param context
     * 
     * @return
     * 
     * @throws ConstraintException
     * @throws RuntimeError
     * @throws IllegaleStateException if {@link #hasPosition()} is false
     */
    public int getEffectiveToPosition(Context context) throws ConstraintException, RuntimeError
    {
        if (this.toPosition != -1) {
            return this.toPosition;
        }
        if (this.toPositionVariable != null) {
            return this.variable2Position(this.toPositionVariable, context);
        }
        
        throw new IllegalStateException(
            "Cannot compute effetive toPosition when not accessing Variable bitwise"
        );
    }
    
    
    /**
     * Ascertains a position value from the given position variable.
     * 
     * @param variable Either {@link #fromPositionVariable} or {@link #toPositionVariable}
     * @param context
     * 
     * @return
     * 
     * @throws ConstraintException
     * @throws RuntimeError
     */
    private int variable2Position(Variable variable, Context context) throws ConstraintException, RuntimeError
    {
        BigInteger value = context.frame.getNumericValue(variable.name).read(context);
        int position;
        try {
            position = value.intValueExact();
            if (
                !Constraints.isValidPosition(position)
                || context.frame.getNumericValue(this.variable.name).length <= position
            ) {
                throw new ArithmeticException("dummy");
            }
            
        } catch (ArithmeticException e) {
            throw new ConstraintException(
                "Value stored in " + variable.name
                + " is used as bitwise access position, but is too "
                + (value.signum() >= 0 ? "big" : "small") + " (is " + value + ")"
            );
        }
        
        return position;
    }
}
