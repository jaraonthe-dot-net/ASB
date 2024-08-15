package net.jaraonthe.java.asb.parse;

import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.exception.ParseError;

/**
 * This contains various semantic constraints for the ASB language, which are
 * used in various locations.<br>
 * 
 * Constraints are put in this central location to a) reduce the size of other
 * classes, and b) to have a central location esp. for constraints that are
 * used in more than one location. Some constraints (esp. those that take little
 * code to implement) may not be included here, though.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Constraints
{
    /* LENGTH */
    
    /**
     * Minimum size of length values. Any length value must be at least this
     * and no greater than {@link #MAX_LENGTH}.
     */
    public static final int MIN_LENGTH = 1;
    
    /**
     * Maximum size of length values. Any length value must be a least
     * {@link #MIN_LENGTH} and no greater than this.
     */
    public static final int MAX_LENGTH = 8192; // i.e. 1kiB

    /**
     * @param length
     * @return True if length is a valid length value
     */
    public static boolean isValidLength(int length)
    {
        return length >= Constraints.MIN_LENGTH && length <= Constraints.MAX_LENGTH;
    }
    
    /**
     * Checks if given value is a valid length, throws exception otherwise.
     * 
     * @param length
     * @throws ParseError if given value is not a valid length
     */
    public static void checkLength(int length) throws ParseError
    {
        Constraints.checkLength(length, null, null);
    }
    
    /**
     * Checks if given value is a valid length, throws exception otherwise.
     * 
     * @param length
     * @param elementName Used in the error message. May be null
     * 
     * @throws ParseError if given value is not a valid length
     */
    public static void checkLength(int length, String elementName) throws ParseError
    {
        Constraints.checkLength(length, elementName, null);
    }
    
    /**
     * Checks if given value is a valid length, throws exception otherwise.
     * 
     * @param length
     * @param elementName Used in the error message. May be null
     * @param origin      Used in the error message. May be null
     * 
     * @throws ParseError if given value is not a valid length
     */
    public static void checkLength(int length, String elementName, Origin origin) throws ParseError
    {
        if (Constraints.isValidLength(length)) {
            return;
        }
        
        throw Constraints.lengthError(String.valueOf(length), elementName, origin);
    }
    
    /**
     * Creates a ParseError signaling that a length value is invalid.
     * 
     * @param length      as a String, thus numbers that are too big for int can
     *                    be provided as well.
     * @param elementName May be null
     * @param origin      May be null
     * 
     * @return
     */
    public static ParseError lengthError(String length, String elementName, Origin origin)
    {
        return new ParseError(
            (elementName != null ? elementName + " length" : "Length")
            + " must be in [" + Constraints.MIN_LENGTH + ", " + Constraints.MAX_LENGTH + "], is "
            + length + (origin != null ? " at " + origin : "")
        );
    }
    
    
    /* POSITION */
    
    /**
     * Minimum value for positions. Any position value must be at least this
     * and no greater than {@link #MAX_POSITION}.
     */
    public static final int MIN_POSITION = 0;
    
    /**
     * Maximum value for positions. Any position value must be a least
     * {@link #MIN_POSITION} and no greater than this.
     */
    public static final int MAX_POSITION = Constraints.MAX_LENGTH - 1;

    /**
     * @param position
     * @return True if position is a valid position value
     */
    public static boolean isValidPosition(int position)
    {
        return position >= Constraints.MIN_POSITION && position <= Constraints.MAX_POSITION;
    }
    
    /**
     * Checks if given value is a valid position, throws exception otherwise.
     * 
     * @param position
     * @throws ParseError if given value is not a valid position
     */
    public static void checkPosition(int position) throws ParseError
    {
        Constraints.checkPosition(position, null);
    }
    
    /**
     * Checks if given value is a valid position, throws exception otherwise.
     * 
     * @param position
     * @param origin   Used in the error message. May be null
     * 
     * @throws ParseError if given value is not a valid position
     */
    public static void checkPosition(int position, Origin origin) throws ParseError
    {
        if (Constraints.isValidPosition(position)) {
            return;
        }
        
        throw Constraints.positionError(String.valueOf(position), origin);
    }
    
    /**
     * Creates a ParseError signaling that a position value is invalid.
     * 
     * @param position as a String, thus numbers that are too big for int can be
     *                 provided as well.
     * @param origin   May be null
     * 
     * @return
     */
    public static ParseError positionError(String position, Origin origin)
    {
        return new ParseError(
            "Position must be in [" + Constraints.MIN_POSITION + ", " + Constraints.MAX_POSITION
            + "], is " + position + (origin != null ? " at " + origin : "")
        );
    }
    
    /**
     * Checks if given position fits within the given variable's length, throws
     * exception otherwise.<br>
     * 
     * Note: If register is a local variable using a dynamic length this will
     * not throw an exception.
     * 
     * @param position
     * @param register
     * @param origin   May be null
     * 
     * @throws ParseError if given position is too big for register
     */
    public static void checkPositionWithinRegister(int position, VariableLike register, Origin origin) throws ParseError
    {
        if (register.maxLength < 1) {
            return;
        }
        
        if (position >= register.maxLength) {
            throw new ParseError(
                "Position " + position + " is too big for Variable "
                + register.toString() + (origin != null ? " at " + origin : "")
            );
        }
    }
}
