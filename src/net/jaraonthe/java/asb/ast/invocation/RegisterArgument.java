package net.jaraonthe.java.asb.ast.invocation;

import java.math.BigInteger;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.Context;
import net.jaraonthe.java.asb.parse.Constraints;

/**
 * An invocation argument that refers to a register or parameter or local
 * variable.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class RegisterArgument extends Argument
{
    /**
     * The register or parameter or local variable this refers to
     */
    public final VariableLike register;
    
    /**
     * Start position of bitwise access.<br>
     * -1 if bitwise access is not used or {@link #fromPositionRegister} is
     * used instead to dynamically supply this value.
     */
    public final int fromPosition;

    /**
     * End position of bitwise access.<br>
     * -1 if bitwise access is not used or {@link #toPositionRegister} is
     * used instead to dynamically supply this value.
     */
    public final int toPosition;
    
    /**
     * Variable used as start position of bitwise access.<br>
     * Null if bitwise access is not used or {@link #fromPosition} is
     * used as value instead.
     */
    public final VariableLike fromPositionRegister;

    /**
     * Variable used as end position of bitwise access.<br>
     * Null if bitwise access is not used or {@link #toPosition} is
     * used as value instead.
     */
    public final VariableLike toPositionRegister;
    

    /**
     * @param register The register or parameter or local variable this argument
     *                 refers to
     */
    public RegisterArgument(VariableLike register)
    {
        this(register, -1, -1, null, null);
    }

    /**
     * @param register The register or parameter or local variable this argument
     *                 refers to
     * @param position The single bit position that is being accessed
     */
    public RegisterArgument(VariableLike register, int position)
    {
        this(register, position, position, null, null);
    }

    /**
     * @param register     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The start of the position range that is being accessed
     * @param toPosition   The end of the position range that is being accessed
     */
    public RegisterArgument(VariableLike register, int fromPosition, int toPosition)
    {
        this(register, fromPosition, toPosition, null, null);
    }

    /**
     * @param register The register or parameter or local variable this argument
     *                 refers to
     * @param position The variable which's value will be used as the single bit
     *                 position that is being accessed
     */
    public RegisterArgument(VariableLike register, VariableLike position)
    {
        this(register, -1, -1, position, position);
    }

    /**
     * @param register     The register or parameter or local variable this
     *                     argument refers to
     * @param register
     * @param fromPosition The variable which's value will be used as the start
     *                     of the position range that is being accessed
     * @param toPosition   The variable which's value will be used as the end
     *                     of the position range that is being accessed
     */
    public RegisterArgument(VariableLike register, VariableLike fromPosition, VariableLike toPosition)
    {
        this(register, -1, -1, fromPosition, toPosition);
    }

    /**
     * @param register     The register or parameter or local variable this
     *                     argument refers to
     * @param fromPosition The start of the position range that is being accessed
     * @param toPosition   The variable which's value will be used as the end
     *                     of the position range that is being accessed
     */
    public RegisterArgument(VariableLike register, int fromPosition, VariableLike toPosition)
    {
        this(register, fromPosition, -1, null, toPosition);
    }

    /**
     * @param register     The register or parameter or local variable this
     *                     argument refers to
     * @param register
     * @param fromPosition The variable which's value will be used as the start
     *                     of the position range that is being accessed
     * @param toPosition   The end of the position range that is being accessed
     */
    public RegisterArgument(VariableLike register, VariableLike fromPosition, int toPosition)
    {
        this(register, -1, toPosition, fromPosition, null);
    }
    
    
    /**
     * @param register
     * @param fromPosition
     * @param toPosition
     * @param fromPositionRegister
     * @param toPositionRegister
     */
    private RegisterArgument(
        VariableLike register,
        int fromPosition,
        int toPosition,
        VariableLike fromPositionRegister,
        VariableLike toPositionRegister
    ) {
        this.register             = register;
        this.fromPosition         = fromPosition;
        this.toPosition           = toPosition;
        this.fromPositionRegister = fromPositionRegister;
        this.toPositionRegister   = toPositionRegister;

        // Check arguments for consistency
        if (this.hasPosition() == (this.toPosition == -1 && this.toPositionRegister == null)) {
            throw new IllegalArgumentException(
                "Inconsistent position settings. Probably used -1 or null somewhere"
            );
        }
        
        if (this.hasPosition()) {
            if (!register.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use bitwise access on non-numeric variable " + register.name
                );
            }
            
            // TODO Also check that positions are within variable length (unless dynamic length)
            // from
            if (this.fromPosition != -1) {
                if (!Constraints.isValidPosition(fromPosition)) {
                    throw new IllegalArgumentException(
                        "Invalid argument " + register.name + " start position. Given value: " + fromPosition
                    );
                }
            } else if (!this.fromPositionRegister.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use non-numeric variable " + this.fromPositionRegister.name
                    + " as bitwise access position"
                );
            }
            
            // to
            if (this.toPosition != -1) {
                if (!Constraints.isValidPosition(toPosition)) {
                    throw new IllegalArgumentException(
                        "Invalid argument " + register.name + " end position. Given value: " + toPosition
                    );
                }
            } else if (!this.toPositionRegister.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use non-numeric variable " + this.toPositionRegister.name
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
        return this.fromPosition != -1 || this.fromPositionRegister != null;
    }
    
    /**
     * @see #hasPosition()
     * @return True if this argument has positional access settings which are
     *         using dynamic position.
     */
    public boolean hasDynamicPosition()
    {
        return this.fromPositionRegister != null || this.toPositionRegister != null;
    }
    
    /**
     * @return The minimum length this argument may represent. Returns
     *         {@link Constraints.MIN_LENGTH MIN_LENGTH} if dynamic length
     *         or register-based bitwise access is used. If the referenced
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
        
        if (this.register.minLength == 0) {
            // dynamic length
            return Constraints.MIN_LENGTH;
        }
        return this.register.minLength;
    }
    
    /**
     * @return The maximum length this argument may represent. May returns
     *         {@link Constraints.MAX_LENGTH MAX_LENGTH} if dynamic length
     *         or register-based bitwise access is used. If the referenced
     *         Variable is non-numeric the return value is undefined.
     */
    public int getMaxLength()
    {
        if (this.hasPosition()) {
            if (this.hasDynamicPosition()) {
                // assume both to and from are dynamic
                int position = 0;
                if (this.fromPositionRegister != null) {
                    if (this.toPositionRegister == null) {
                        // from is dynamic, to is fixed
                        position = this.toPosition;
                    }
                } else if (this.toPositionRegister == null) {
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
     * maximum length of the reference register limits what is possible.
     * 
     * @param position
     * @return
     */
    private int calculateMaxLength(int position)
    {
        int length = this.register.maxLength;
        if (this.register.maxLength < 1) {
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
    public Variable.Type getVariableType()
    {
        return Variable.Type.REGISTER;
    }
    
    @Override
    public String toString()
    {
        if (!this.hasPosition()) {
            return this.register.name;
        }
        
        String position = "";
        if (this.fromPosition != -1) {
            position += this.fromPosition;
            
            if (this.toPositionRegister != null) {
                position += ":" + this.toPositionRegister.name;
            } else if (this.toPosition != this.fromPosition) {
                position += ":" + this.toPosition;
            }
            
        } else {
            position += this.fromPositionRegister.name;
            
            if (this.toPosition != -1) {
                position += ":" + this.toPosition;
            } else if (this.toPositionRegister != this.fromPositionRegister) {
                position += ":" + this.toPositionRegister.name;
            }
        }
        
        return this.register.name + "'" + position;
    }

    
    /**
     * Calculates the effective start position of bitwise access.
     * 
     * This also resolves dynamic position (via position register) using the
     * given interpretation context.
     * 
     * @param context
     * 
     * @return
     * 
     * @throws RuntimeError
     * @throws IllegaleStateException if {@link #hasPosition()} is false
     */
    public int getEffectiveFromPosition(Context context) throws RuntimeError
    {
        if (this.fromPosition != -1) {
            return this.fromPosition;
        }
        if (this.fromPositionRegister != null) {
            return this.register2Position(this.fromPositionRegister, context);
        }
        
        throw new IllegalStateException(
            "Cannot compute effetive fromPosition when not accessing Variable bitwise"
        );
    }
    
    /**
     * Calculates the effective end position of bitwise access.
     * 
     * This also resolves dynamic position (via position register) using the
     * given interpretation context.
     * 
     * @param context
     * 
     * @return
     * 
     * @throws RuntimeError
     * @throws IllegaleStateException if {@link #hasPosition()} is false
     */
    public int getEffectiveToPosition(Context context) throws RuntimeError
    {
        if (this.toPosition != -1) {
            return this.toPosition;
        }
        if (this.toPositionRegister != null) {
            return this.register2Position(this.toPositionRegister, context);
        }
        
        throw new IllegalStateException(
            "Cannot compute effetive toPosition when not accessing Variable bitwise"
        );
    }
    
    
    /**
     * Ascertains a position value from the given position register.
     * 
     * @param register Either {@link #fromPositionRegister} or {@link #toPositionRegister}
     * @param context
     * 
     * @return
     * 
     * @throws RuntimeError
     */
    private int register2Position(VariableLike register, Context context) throws RuntimeError
    {
        BigInteger value = context.frame.getNumericValue(register.name).read(context);
        int position;
        try {
            position = value.intValueExact();
            if (
                !Constraints.isValidPosition(position)
                || context.frame.getNumericValue(this.register.name).length <= position
            ) {
                throw new ArithmeticException("dummy");
            }
            
        } catch (ArithmeticException e) {
            throw new RuntimeError(
                "Value stored in " + register.name
                + " is used as bitwise access position, but is too "
                + (value.signum() >= 0 ? "big" : "small") + " (is " + value + ")"
            );
        }
        
        return position;
    }
}
