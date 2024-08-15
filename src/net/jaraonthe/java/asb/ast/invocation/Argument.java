package net.jaraonthe.java.asb.ast.invocation;

import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.parse.Token;

/**
 * An argument used in an invocation (after invocation has been resolved).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
abstract public class Argument
{
    /**
     * @return The Variable type corresponding to this argument.
     */
    abstract public Variable.Type getVariableType();
    
    
    /**
     * Creates an argument from the given Token.<br>
     * 
     * Note: A {@link RegisterArgument} cannot be created with this, as that is
     * more complex.
     * 
     * @param token with type: NAME, LABEL_NAME, NUMBER, or STRING
     * @return
     */
    public static Argument fromToken(Token token)
    {
        switch (token.type) {
            case NAME:
                return new RawArgument(token.content);
                
            case LABEL_NAME:
                // TODO support label
                throw new RuntimeException("labels not yet implemented");
                
            case NUMBER:
            // immediate
                return new ImmediateArgument(Token.number2BigInteger(token));
                
            case STRING:
            // string
                return new StringArgument(token.content);
            
            default:
                throw new IllegalArgumentException(
                    "Unsupported Token type " + token.type + " as Invocation argument"
                );
        }
    }
}
