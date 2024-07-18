package net.jaraonthe.java.asb.parse;

import java.util.Locale;

/**
 * Represents one Token as produced by the Tokenizer.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Token
{
	public enum Type
	{
		// * - only in meta mode
		
		// with content:
		COMMAND_SYMBOLS,
		DATATYPE,        // /<content> *
		DIRECTIVE,       // .<content>
		FUNCTION_NAME,   // &<content>
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
		STATEMENT_SEPARATOR, // ; or \n

		// only in expression mode:
		EXP_ADD,                    // +
		EXP_EQUALS,                 // ==
		EXP_GREATER_THAN,           // >
		EXP_GREATER_THAN_OR_EQUALS, // >=
		EXP_LESS_THAN,              // <
		EXP_LESS_THAN_OR_EQUALS,    // <=
		EXP_NOT_EQUALS,             // !=
		EXP_RANGE,                  // :
		EXP_SUBTRACT,               // -
	}
	
	public final Token.Type type;
	
	/**
	 * The meaning of the content depends on the Token's type.
	 * 
	 * May be null.<br>
	 * 
	 * - DATATYPE:      datatype name, without leading /<br>
     * - DIRECTIVE:     directive name, without leading .<br>
     * - FUNCTION_NAME: function name, without leading &<br>
     * - LABEL:         label name, without trailing :<br>
     * - STRING:        string content (processed), witout leading and trailing "<br>
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
	 * @param content
	 * @param origin
	 */
	public Token(Token.Type type, String content, Origin origin)
	{
		this.type    = type;
		if (content != null && type != Token.Type.STRING) {
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
        return this.type + "(" + (this.content != null ? this.content : "") + ")";
	}

	/**
	 * @return String representation including Origin (aka position) of this
	 *                Token.
	 */
	public String toStringWithOrigin()
    {
        return this.toString() +" at " + this.origin;
    }
}
