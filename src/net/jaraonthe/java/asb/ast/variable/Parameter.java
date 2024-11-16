package net.jaraonthe.java.asb.ast.variable;

import net.jaraonthe.java.asb.parse.Constraints;

/**
 * A Command Parameter.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
// TODO Consider splitting into numeric and other parameters (label, string) or
//      possibly even further
public class Parameter extends Variable
{
    /**
     * Every Parameter has a type.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum Type
    {
        /**
         * Register or local variable parameter (which are treated the same)
         */
        REGISTER('\u0011', "/register", Parameter.Type.Length.RANGE, true),
        
        /**
         * An immediate numeric value parameter
         */
        IMMEDIATE('\u0012', "/immediate", Parameter.Type.Length.EXACT, false),
        
        /**
         * A label parameter pointing to a position in the program
         */
        LABEL('\u0011', "/label", Parameter.Type.Length.EXACT, false),
        // LABEL has same signatureMarker as REGISTER, as in the
        // resolvingSignature they  have to be the same (because syntactically
        // they cannot be distinguished in the invocation without going through
        // the resolving process). In signature they can be distinguished
        // because one has length but not the other (this is hardcoded).
        
        /**
         * A string parameter. This type cannot be used freely in the ASB
         * language, only with some built-in functions.
         */
        STRING('\u0013', "/string", Parameter.Type.Length.NO, false);
        
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
        public final Parameter.Type.Length length;
        
        /**
         * True if this type of parameter supports a group setting.
         */
        public final boolean supportsGroup;
        
        private Type(
            char signatureMarker,
            String readableSignaturePlaceholder,
            Parameter.Type.Length length,
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
         * This supports all Parameter type names that can occur in ASB Syntax.
         * 
         * @param type
         * @return Corresponding Type object, or null if none fits.
         */
        public static Parameter.Type fromString(String type)
        {
            switch (type) {
                case "register":
                case "reg":
                case "variable":
                case "var":
                    return Parameter.Type.REGISTER;
                case "immediate":
                case "imm":
                    return Parameter.Type.IMMEDIATE;
                case "label":
                    return Parameter.Type.LABEL;
                // The STRING type can only be used by built-in functions
                default:
                    return null;
            }
        }
        
        /**
         * @return True if this type supports length settings.<br>
         *         Note that this is true for LABEL Types, though you may want
         *         to treat that type in a special way.
         */
        public boolean hasLength()
        {
            return this.length != Parameter.Type.Length.NO;
        }
    }
    
    public static final int LOCAL_LABEL_LENGTH = 31;
    

    public final Parameter.Type type;
    
    /**
     * For LABEL type only. True: This parameter is resolved to a label local to
     * the invocation (can only be used by built-in functions). False: This
     * label is resolved to userland code (default).
     */
    public final boolean localLabel;
    
    private String group = null;
    
    
    /**
     * Create with a type that doesn't support length.
     * 
     * @param type Type that doesn't support length setting
     * @param name
     */
    public Parameter(Parameter.Type type, String name)
    {
        this(type, name, -1, -1, false);
    }
    
    /**
     * Create with a type that supports exact (or range) length.
     * 
     * @param type   Type that supports exact or range length setting
     * @param name
     * @param length exact length setting
     */
    public Parameter(Parameter.Type type, String name, int length)
    {
        this(type, name, length, length, false);
    }
    
    /**
     * Create a Parameter with a length range.
     * 
     * @param type      Type that supports range length setting
     * @param name
     * @param minLength <= maxLength
     * @param maxLength
     */
    public Parameter(Parameter.Type type, String name, int minLength, int maxLength)
    {
        this(type, name, minLength, maxLength, false);
    }
    
    /**
     * Create a Label parameter.
     * 
     * @param type       Must be LABEL
     * @param name
     * @param length     exact length setting (usually either pc length or
     *                   {@link #LOCAL_LABEL_LENGTH})
     * @param localLabel True if this label is resolved locally
     */
    public Parameter(Parameter.Type type, String name, int length, boolean localLabel)
    {
        this(type, name, length, length, localLabel);
    }
    
    /**
     * @param type
     * @param name
     * @param minLength
     * @param maxLength
     * @param localLabel
     */
    private Parameter(
        Parameter.Type type,
        String name,
        int minLength,
        int maxLength,
        boolean localLabel
    ) {
        super(name, minLength, maxLength);
        this.type       = type;
        this.localLabel = localLabel;
        
        if (localLabel && type != Parameter.Type.LABEL) {
            throw new IllegalArgumentException(
                "localLabel can be only be true for LABEL type, but type is " + type
            );
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
     * Sets the group for this parameter. If a group is set, only variables that
     * have this group can be used as an argument for this parameter.
     * 
     * @param group
     * @return Fluent interface
     */
    public Parameter setGroup(String group)
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
