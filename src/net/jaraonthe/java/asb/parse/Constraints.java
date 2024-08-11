package net.jaraonthe.java.asb.parse;

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
    /**
     * Maximum size of length values. Any length value must be a least 1 and
     * at maximum the value configured here.
     */
    public static final int MAX_LENGTH = 8192; // i.e. 1kiB

    /**
     * @param length
     * @return True if length is a valid length value
     */
    public static boolean isValidLength(int length)
    {
        return length > 0 && length <= Constraints.MAX_LENGTH;
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
            + " must be in [1, " + Constraints.MAX_LENGTH + "], is " + length
            + (origin != null ? " at " + origin : "")
        );
    }
}
