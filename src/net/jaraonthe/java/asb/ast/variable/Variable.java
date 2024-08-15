package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A Local Variable or Command Parameter.<br>
 * 
 * Note that this class mostly deals with parameter specification, but also
 * represents the (much more limited) local variable to keep things "simple".
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
// TODO Consider splitting into numeric variables and other variables (label,
//      string) or possibly even further
public class Variable extends VariableLike
{
    /**
     * Every Variable has a type.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum Type
    {
        /**
         * Register or variable parameter (which are treated the same)
         */
        REGISTER('\u0011', "/register", Variable.Type.Length.RANGE, true),
        
        /**
         * An immediate numeric value parameter
         */
        IMMEDIATE('\u0012', "/immediate", Variable.Type.Length.EXACT, false),
        
        /**
         * A label parameter pointing to a location in the program
         */
        LABEL('\u0011', "/label", Variable.Type.Length.NO, false),
        // LABEL has same signatureMarker as REGISTER, as in the
        // resolvingSignature they  have to be the same (because syntactically
        // they cannot be distinguished in the invocation without going through
        // the resolving process). In signature they can be distinguished
        // because one has length but not the other.
        
        /**
         * A string parameter. This type cannot be used freely in the ASB
         * language, only with some built-in functions.
         */
        STRING('\u0013', "/string", Variable.Type.Length.NO, false),
        
        /**
         * A local variable, which functions like a register except its value is
         * scoped within the command implementation.<br>
         * This cannot be used as a Parameter in a command signature, as that
         * wouldn't make sense.
         */
        LOCAL_VARIABLE('\u0011', ".var", Variable.Type.Length.EXACT, false);
        
        public enum Length
        {
            /**
             * This type doesn't support a length setting.
             */
            NO,
            /**
             * This type supports an exact length.
             */
            EXACT,
            /**
             * This type supports an exact length or a length range.
             */
            RANGE,
        }
        
        /**
         * Used in a command signature to mark the end of parameter markers that
         * contain details (i.e. length, group).
         */
        public static final char END_OF_MARKER_DETAILS = '\u0003';
        
        
        /**
         * Used to mark a parameter of this type in a command signature.
         */
        public final char signatureMarker;
        
        /**
         * Used to mark a parameter of this type in a readable command signature.
         */
        public final String readableSignaturePlaceholder;
        
        /**
         * Denotes if and which length settings this type supports.
         */
        public final Variable.Type.Length length;
        
        /**
         * True if this type of parameter supports a group setting.
         */
        public final boolean supportsGroup;
        
        private Type(
            char signatureMarker,
            String readableSignaturePlaceholder,
            Variable.Type.Length length,
            boolean supportsGroup
        ) {
            this.signatureMarker              = signatureMarker;
            this.readableSignaturePlaceholder = readableSignaturePlaceholder;
            this.length                       = length;
            this.supportsGroup                = supportsGroup;
        }
        
        /**
         * Provides the corresponding Type object for the given type name.
         * 
         * This supports all Variable type names that can occur in ASB Syntax.
         * 
         * @param type
         * @return Corresponding Type object, or null if none fits.
         */
        public static Variable.Type fromString(String type)
        {
            switch (type) {
                case "register":
                case "reg":
                case "variable":
                case "var":
                    return Variable.Type.REGISTER;
                case "immediate":
                case "imm":
                    return Variable.Type.IMMEDIATE;
                case "label":
                    return Variable.Type.LABEL;
                // The STRING type can only be used by built-in functions
                default:
                    return null;
            }
        }
        
        /**
         * @return True if this type supports length settings.
         */
        public boolean hasLength()
        {
            return this.length != Variable.Type.Length.NO;
        }
    }
    
    

    /**
     * If this is not null, then the referenced Variable is the source for this
     * Local Variable's length setting (which is determined dynamically). In
     * this case {@link VariableLike#minLength minLength} and
     * {@link VariableLike#maxLength maxLength} are meaningless.<br>
     * 
     * Can only be used when {@link #type} is
     * {@link Variable.Type#LOCAL_VARIABLE LOCAL VARIABLE}.
     */
    public final VariableLike lengthRegister;
    
    public final Variable.Type type;
    
    private String group = null;
    
    
    /**
     * Create with a type that doesn't support length.
     * 
     * @param type Type that doesn't support length setting
     * @param name
     */
    public Variable(Variable.Type type, String name)
    {
        this(type, name, -1, -1, null);
    }
    
    /**
     * Create with a type that supports exact (or range) length.
     * 
     * @param type   Type that supports exact or range length setting
     * @param name
     * @param length exact length setting
     */
    public Variable(Variable.Type type, String name, int length)
    {
        this(type, name, length, length, null);
    }
    
    /**
     * Create a Variable with a length range.
     * 
     * @param type      Type that supports range length setting
     * @param name
     * @param minLength <= maxLength
     * @param maxLength
     */
    public Variable(Variable.Type type, String name, int minLength, int maxLength)
    {
        this(type, name, minLength, maxLength, null);
    }
    
    /**
     * Create a Variable with a length register instead of static length
     * setting.
     * 
     * @param type           Must be LOCAL_VARIABLE
     * @param name
     * @param lengthRegister
     */
    public Variable(Variable.Type type, String name, VariableLike lengthRegister)
    {
        this(type, name, 0, 0, lengthRegister);
    }
    
    /**
     * @param type
     * @param name
     * @param minLength
     * @param maxLength
     * @param lengthRegister
     */
    private Variable(
        Variable.Type type,
        String name,
        int minLength,
        int maxLength,
        VariableLike lengthRegister
    ) {
        super(name, minLength, maxLength);
        this.type = type;
        this.lengthRegister = lengthRegister;
        
        if (type == Variable.Type.LOCAL_VARIABLE && lengthRegister != null) {
            if (!lengthRegister.isNumeric()) {
                throw new IllegalArgumentException(
                    "Cannot use " + lengthRegister + " parameter as length register"
                );
            }
            return;
        }
        
        switch (type.length) {
            case NO:
                if (minLength != -1 || maxLength != -1) {
                    throw new IllegalArgumentException(
                        "Must not set length for " + type + " parameter"
                    );
                }
                break;
            case EXACT:
                if (minLength != maxLength) {
                    throw new IllegalArgumentException(
                        "minLength and maxLength must be equal for " + type + " parameter, but are "
                        + minLength + " and " + maxLength + ", respectively"
                    );
                }
                if (!Constraints.isValidLength(minLength)) {
                    throw new IllegalArgumentException(
                        "Invalid parameter " + name + " length. Given value: " + minLength
                    );
                }
                break;
            case RANGE:
                if (minLength > maxLength) {
                    throw new IllegalArgumentException(
                        "minLength must not be greater than maxLength for " + type
                        + " parameter, but are " + minLength + " and " + maxLength + ", respectively"
                    );
                }
                if (!Constraints.isValidLength(minLength)) {
                    throw new IllegalArgumentException(
                        "Invalid parameter " + name + " minimum length. Given value: " + minLength
                    );
                }
                if (!Constraints.isValidLength(minLength)) {
                    throw new IllegalArgumentException(
                        "Invalid parameter " + name + " maximum length. Given value: " + minLength
                    );
                }
                break;
        }
    }
    
    /**
     * Sets the group for this parameter. If a group is set, only registers that
     * have this group can be used as an argument for this parameter.
     * 
     * @param group
     * @return Fluent interface
     */
    public Variable setGroup(String group)
    {
        if (!this.type.supportsGroup) {
            throw new IllegalStateException(
                this.type + " parameter type does not support a group"
            );
        }
        if (this.group != null) {
            throw new IllegalStateException(
                "Cannot set parameter group more than once"
            );
        }
        
        this.group = group;
        return this;
    }
    
    /**
     * @return True if this parameter has a group configured.
     */
    public boolean hasGroup()
    {
        return this.group != null;
    }
    
    @Override
    public boolean hasGroup(String group)
    {
        return group.equals(this.group);
    }
    
    /**
     * @return The name of the configured group. Null if no group configured.
     */
    public String getGroup()
    {
        return this.group;
    }
    
    @Override
    public String toString()
    {
        return this.type.readableSignaturePlaceholder + " " + super.toString()
            + (this.hasGroup() ? "(" + this.group + ")" : "");
    }
}
