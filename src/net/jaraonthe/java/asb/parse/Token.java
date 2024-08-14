package net.jaraonthe.java.asb.parse;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Pattern;

import net.jaraonthe.java.asb.exception.ConstraintException;

/**
 * Represents one Token as produced by the Tokenizer.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Token
{
    /**
     * Every Token has a type.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum Type
    {
        // * - only in META mode
        
        // with content:
        COMMAND_SYMBOLS,
        DATATYPE,        // /<content> *
        DIRECTIVE,       // content = ".<name>"
        FUNCTION_NAME,   // content = "&<name>"
        LABEL,           // <content>:
        LABEL_NAME,      // if NAME doesn't fit (because it starts with a number or a .)
        NAME,
        NUMBER,
        STRING,          // "<content>" - content has been processed
        
        // no content:
        BIT_LENGTH,          // ''
        BIT_POSITION,        // '
        CLOSING_BRACES,      // } *
        OPENING_BRACES,      // { *
        POSITION_RANGE,      // :
        STATEMENT_SEPARATOR, // ; or \n
        
        // only in LENGTH mode:
        LENGTH_GREATER_THAN_OR_EQUALS, // >=
        LENGTH_LESS_THAN_OR_EQUALS,    // <=
        LENGTH_MAX,                    // max
        LENGTH_MAXU,                   // maxu
        LENGTH_RANGE,                  // ..
        
        EOF,
    }
    
    
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
    
    
    public final Token.Type type;
    
    /**
     * The meaning of the content depends on the Token's type.
     * 
     * May be null.<br>
     * 
     * - DATATYPE:      datatype name, without leading /<br>
     * - DIRECTIVE:     directive name, with leading .<br>
     * - FUNCTION_NAME: function name, with leading &<br>
     * - LABEL:         label name, without trailing :<br>
     * - STRING:        string content (processed), without leading and trailing "<br>
     * - all others:    verbatim as given<br>
     * 
     * Note that all content (except for STRING) is stored in lowercase in order
     * to support case-insensitivity.
     */
    public final String content;
    
    /**
     * The location where this token is coming from (file, line, and col in line).
     */
    public final Origin origin;
    
    
    /**
     * @param type
     * @param content Will be stored in lower case unless type is STRING. For
     *                type DIRECTIVE or FUNCTION_NAME: Expected without leading
     *                . or & symbol, but will be stored with.
     * @param origin
     */
    @SuppressWarnings("incomplete-switch")
    public Token(Token.Type type, String content, Origin origin)
    {
        this.type    = type;
        if (content != null && type != Token.Type.STRING) {
            switch (type) {
                case DIRECTIVE:
                    content = "." + content;
                    break;
                case FUNCTION_NAME:
                    content = "&" + content;
                    break;
            }
            
            // This is where the language becomes case-insensitive
            this.content = content.toLowerCase(Locale.ROOT);
        } else {
            this.content = null;
        }
        this.origin  = origin;
    }
    
    /**
     * Creates a Token without content.
     * 
     * @param type
     * @param origin
     */
    public Token(Token.Type type, Origin origin)
    {
        this(type, null, origin);
    }
    
    @Override
    public String toString()
    {
        return this.type + (this.content != null ? "(" + this.content + ")" : "");
    }

    /**
     * @return String representation including Origin (aka position) of this
     *                Token.
     */
    public String toStringWithOrigin()
    {
        return this.toString() + " at " + Token.getOriginAsString(this);
    }

    /**
     * Provides a Token's Origin as string.
     * 
     * This method is null-safe.
     * 
     * @param token May be null
     * @return
     */
    public static String getOriginAsString(Token token)
    {
        if (token == null) {
            return "?";
        }
        return token.origin.toString();
    }
    
    /**
     * Provides a Token's origin.
     * 
     * This method is null-safe, i.e. no error occurs when token is null.
     * 
     * @param token May be null
     * @return token's origin or null.
     */
    public static Origin getOrigin(Token token)
    {
        if (token == null) {
            return null;
        }
        return token.origin;
    }
    
    
    /**
     * Provides a Token's type.
     * 
     * This method is null-safe, i.e. no error occurs when token is null.
     * 
     * @param token May be null
     * @return token's type or null.
     */
    public static Token.Type getType(Token token)
    {
        if (token == null) {
            return Token.Type.EOF;
        }
        return token.type;
    }
    
    /**
     * Transforms a NUMBER token's content to an integer value.<br>
     * 
     * Note: This should only be used for length values, as due to global
     * constraint {@link Constraints#MAX_LENGTH} they always fit within an int;
     * but this is not necessarily true for any other numeric value.
     * 
     * @param token a NUMBER token
     * @throws ConstraintException if provided number is too big for int
     */
    public static int number2Int(Token token) throws ConstraintException
    {
        if (!Token.getType(token).equals(Token.Type.NUMBER)) {
            throw new IllegalArgumentException("Not a NUMBER token");
        }
        
        // Strip _ chars
        String content = Token.UNDERSCORE_PATTERN.matcher(token.content).replaceAll("");
        
        char c0 = content.charAt(0);
        try {
            if (c0 == '0') {
                if (content.length() < 2) {
                    return 0;
                }
                switch (content.charAt(1)) {
                    case 'x':
                        // hexadecimal (0x...)
                        return Integer.parseInt(content.substring(2), 16);
                    case 'b':
                        // binary (0b...)
                        return Integer.parseInt(content.substring(2), 2);
                    default:
                        // octal (0...)
                        return Integer.parseInt(content, 8);
                }
            }
            
            // Decimal
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            // Assuming this is thrown because number is too big for int
            throw new ConstraintException("NUMBER " + token.content + " is too big for int type");
        }
    }
    

    
    /**
     * Transforms a NUMBER token's content to a BigInteger value.<br>
     * 
     * This should be used for all immediate values.
     * 
     * @param token a NUMBER token
     * @return
     */
    // TODO What about negative numbers? We either need to know the var length
    //      to extend leading 1s accordingly, or use a negative BigInteger
    //      (which may require special handling in several places)
    //      - currently, a negative BigInteger is created
    public static BigInteger number2BigInteger(Token token)
    {
        if (!Token.getType(token).equals(Token.Type.NUMBER)) {
            throw new IllegalArgumentException("Not a NUMBER token");
        }
        
        // Strip _ chars
        String content = Token.UNDERSCORE_PATTERN.matcher(token.content).replaceAll("");
        
        char c0 = content.charAt(0);
        if (c0 == '0') {
            if (content.length() < 2) {
                return BigInteger.ZERO;
            }
            switch (content.charAt(1)) {
                case 'x':
                    // hexadecimal (0x...)
                    return new BigInteger(content.substring(2), 16);
                case 'b':
                    // binary (0b...)
                    return new BigInteger(content.substring(2), 2);
                default:
                    // octal (0...)
                    return new BigInteger(content, 8);
            }
        }
        
        // Decimal
        return new BigInteger(content);
    }
}
