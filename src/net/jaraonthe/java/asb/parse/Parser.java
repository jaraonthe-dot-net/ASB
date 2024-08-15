package net.jaraonthe.java.asb.parse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.invocation.Argument;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.invocation.RegisterArgument;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VariableLike;
import net.jaraonthe.java.asb.ast.variable.VirtualRegister;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.LexicalError;
import net.jaraonthe.java.asb.exception.ParseError;

/**
 * Parses ASB source code into AST.<br>
 * 
 * Takes care of all semantic rules and constraints. Applies all necessary
 * transformations. The resulting AST can be executed directly.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Parser
{
    /**
     * The AST that will be filled by this Parser.
     */
    public final AST ast = new AST();
    
    /**
     * The currently used Tokenizer (which is coupled to exactly one SourceFile).
     */
    private Tokenizer tokenizer;
    
    // TODO Be aware & handle potential "Fluke" Tokens:
    //        Token           => may be this:
    //      - NAME            => label name
    //      - NUMBER          => label name
    //      - DIRECTIVE       => label name
    
    // TODO Consider re-arranging so that one Parser instance takes care of
    //      exactly one SourceFile (makes coupling to tokenizer easier).
    //      This means:
    //      - ast is set from the outside
    //      - SourceFile is set in constructor.
    //      - when .include (or whatever it is called) directive is encountered,
    //        another Parser instance is created to handle the included file
    //        before continuing the current parse (this is the main reason to
    //        have one Parser per file).
    //      - There should be another class taking care of the overall parsing
    //        process (parsing all input files, doing constraints & transforms
    //        after all file Parsers are done). What to call this class?
    
    
    /**
     * Parses the given file.
     * 
     * @param file
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    public void parseFile(SourceFile file) throws LexicalError, ParseError
    {
        this.tokenizer = new Tokenizer(file);
        
        Token t;
        while ((t = this.tokenizer.next()) != null) {
            switch (t.type) {
                case DIRECTIVE:
                    this.parseDirective(t);
                    break;
                case NAME:
                case FUNCTION_NAME:
                    Invocation invocation = this.parseInvocation(t.content, null);
                    try {
                        invocation.resolve(this.ast, null);
                    } catch (ConstraintException e) {
                        throw new ParseError(e.getMessage() + " at " + t.origin);
                    }
                    try {
                        this.ast.addToProgram(invocation);
                    } catch (ConstraintException e) {
                        throw new ParseError(
                            "Maximum program size exceeded at " + t.origin + ". " + e.getMessage()
                        );
                    }
                    break;
                case LABEL:
                    // TODO labels
                    throw new RuntimeException("labels are not yet supported");
                    //break;
                case STATEMENT_SEPARATOR:
                    // Nothing
                    break;
                default:
                    throw new ParseError("Unexpected " + t.toStringWithOrigin());
            }
        }
    }
    
    
    
    /**
     * Parses a directive.
     * 
     * @param directive The starting directive token (containing the directive name).
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void parseDirective(Token directive) throws LexicalError, ParseError
    {
        this.tokenizer.setMode(Tokenizer.Mode.META);
        
        Token t;
        switch (directive.content) {
            case ".memory":
                // TODO check if first invocation of a command using memory was
                //      already parsed -> configuring memory no longer allowed
                boolean isMultiLine = this.consumeOpeningBraces();
                
                int wordLength = -1;
                int addressLength = -1;
                for (int i = 0; i < 2; i++) {
                    t = this.expect(Token.Type.DIRECTIVE, (wordLength == - 1 ? ".word" : ".address"));
                    
                    switch (t.content) {
                        case ".word":
                            if (wordLength != -1) {
                                throw new ParseError("Cannot use .word more than once at" + t.origin);
                            }
                            wordLength = this.expectLength(t.content);
                        break;
                        case ".address":
                        case ".addr":
                            if (addressLength != -1) {
                                throw new ParseError("Cannot use " + t.content + " more than once at" + t.origin);
                            }
                            addressLength = this.expectLength(t.content);
                            break;
                        default:
                            throw new ParseError("Unexpected \"" + t.content + "\" at " + t.origin);
                    }
                    this.skipIfMultiLine(isMultiLine);
                }
                this.ast.setMemory(wordLength, addressLength);
                
                this.expectClosingBracesIfMultiLine(isMultiLine);
                // TODO add to docs: After closing braces (of a multi-line
                //      directive), either newline or ; must follow for separation
                this.expectStatementSeparator();
                break;

            case ".program_counter":
            case ".pc":
                if (this.ast.hasProgram()) {
                    throw new ParseError(
                        "Cannot configure program counter after first command at" + directive.origin
                    );
                }
                this.ast.setPcLength(this.expectLength(directive.content));
                
                this.expectStatementSeparator();
                break;
            
            case ".register":
            case ".reg":
                this.parseRegister(Parser.RegisterType.REGULAR, directive.origin);
                break;
                
            case ".register_alias":
            case ".reg_alias":
                this.parseRegister(Parser.RegisterType.ALIAS, directive.origin);
                break;
            
            // TODO add all variants to docs
            case ".register_virtual":
            case ".register_virt":
            case ".reg_virtual":
            case ".reg_virt":
            case ".virtual_register":
            case ".virtual_reg":
            case ".virt_register":
            case ".virt_reg":
                this.parseRegister(Parser.RegisterType.VIRTUAL, directive.origin);
                break;
            
            case ".define":
            case ".def":
                this.parseDefine(directive.origin);
                break;
                
            default:
                throw new ParseError("Unknown directive \"" + directive.content + "\" at " + directive.origin);
        }
        
        this.tokenizer.setMode(Tokenizer.Mode.MAIN);
    }
    
    /**
     * Used for {@link #parseRegister()}. Decides whether .register,
     * .register_alias, or a .register_virtual directive is being parsed.
     */
    private enum RegisterType
    {
        REGULAR,
        ALIAS,
        VIRTUAL,
    }
    
    /**
     * Parses a register definition.
     * 
     * Starts consuming AFTER the starting DIRECTIVE Token.
     * 
     * @param type            Selects which kind of register is parsed
     * @param directiveOrigin The origin of the directive Token
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void parseRegister(Parser.RegisterType type, Origin directiveOrigin) throws LexicalError, ParseError
    {
        String name = this.expectName();
        if (this.ast.registerExists(name)) {
            throw new ParseError(
                "Cannot declare register " + name + " more than once at " + directiveOrigin
            );
        }
        
        Register register;
        boolean isMultiLine;
        if (type == Parser.RegisterType.ALIAS) {
            // .register_alias <name>, <aliasedName>
            this.expectCommandSymbols(",");
            String aliasedName = this.expectName();
            Register aliasedRegister = this.ast.getRegister(aliasedName);
            if (aliasedRegister == null) {
                throw new ParseError(
                    "Attempting to declare alias to register " + aliasedName
                    + ", but this register doesn't exist yet at " + directiveOrigin
                );
            }
            register = new RegisterAlias(name, aliasedRegister);
            
        } else {
            // .register <name> ''<length>
            // .register_virtual <name> ''<length> (...)
            int length = this.expectLength("register " + name);
            
            if (type == Parser.RegisterType.REGULAR) {
                register = new Register(name, length);
            } else {
                register = new VirtualRegister(name, length);
            }
        }
        
        isMultiLine = this.consumeOpeningBraces();

        if (type != Parser.RegisterType.VIRTUAL) {
            this.parseGroups(register, isMultiLine);
            
            this.expectClosingBracesIfMultiLine(isMultiLine);
            this.expectStatementSeparator();
        } else {
            
            // .register_virtual (...) ...sub-directives
            VirtualRegister vr = (VirtualRegister)register;
            Token t;
            ArrayList<Variable> parameters;
            while(Token.getType(t = this.tokenizer.peek()) == Token.Type.DIRECTIVE) {
                switch (t.content) {
                    case ".group":
                        this.parseGroups(register, isMultiLine);
                        break;
                    case ".store":
                        if (vr.hasStore()) {
                            throw new ParseError(
                                "Cannot define store for virtual register "
                                + vr.name + " more than once at " + t.origin
                            );
                        }
                        this.tokenizer.next();
                        vr.setStore(this.expectLength("register " + name + " store"));
                        break;
                        
                    case ".get":
                        if (vr.getGetterImplementation() != null) {
                            throw new ParseError(
                                "Cannot define getter for virtual register "
                                + vr.name + " more than once at " + t.origin
                            );
                        }
                        this.tokenizer.next();
                        this.expect(Token.Type.OPENING_BRACES);
                        parameters = new ArrayList<>(1);
                        parameters.add(new Variable(Variable.Type.REGISTER, "out", register.getLength()));
                        if (vr.hasStore()) {
                            // TODO Note in docs that .store has to come before
                            //      the .get and/or .set that use it!
                            parameters.add(new Variable(Variable.Type.REGISTER, "store", vr.getStoreLength()));
                        }
                        vr.setGetterImplementation(this.parseImplementation(parameters));
                        break;
                    case ".set":
                        if (vr.getSetterImplementation() != null) {
                            throw new ParseError(
                                "Cannot define setter for virtual register "
                                + vr.name + " more than once at " + t.origin
                            );
                        }
                        this.tokenizer.next();
                        this.expect(Token.Type.OPENING_BRACES);
                        parameters = new ArrayList<>(1);
                        parameters.add(new Variable(Variable.Type.REGISTER, "in", register.getLength()));
                        if (vr.hasStore()) {
                            parameters.add(new Variable(Variable.Type.REGISTER, "store", vr.getStoreLength()));
                        }
                        vr.setSetterImplementation(this.parseImplementation(parameters));
                        break;
                        
                    default:
                        throw new ParseError(
                            "Unexpected " + t.content + " directive in virtual register definition at " + t.origin
                        );
                }
                this.skipIfMultiLine(isMultiLine);
            }
            this.expectClosingBracesIfMultiLine(isMultiLine);
            this.expectStatementSeparator();
            
            if (vr.getGetterImplementation() == null) {
                throw new ParseError(
                    "A getter must be defined for virtual register "
                    + vr.name + " at " + directiveOrigin
                );
            }
            if (vr.getSetterImplementation() == null) {
                throw new ParseError(
                    "A setter must be defined for virtual register "
                    + vr.name + " at " + directiveOrigin
                );
            }
        }
        
        this.ast.addRegister(register);
    }
    
    /**
     * Parses groups sub-directives for a register definition.<br>
     * 
     * Does a tentative parse, stopping as soon as anything other than a .group
     * sub-directive is encountered.
     * 
     * @param register    The register being worked on
     * @param isMultiLine True if directive is using a multi-line block.
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void parseGroups(Register register, boolean isMultiLine) throws LexicalError, ParseError
    {
        Token t;
        while (Token.getType(t = this.tokenizer.peek()) == Token.Type.DIRECTIVE) {
            switch (t.content) {
                case ".group":
                    this.tokenizer.next();
                    String name = this.expectName();
                    if (register.hasGroup(name)) {
                        throw new ParseError(
                            "Cannot declare group " + name + " more than once for register "
                            + register.name + " at " + t.origin
                        );
                    }
                    register.addGroup(name);
                    break;
                default:
                    return;
            }
            this.skipIfMultiLine(isMultiLine);
        }
    }
    
    
    /**
     * Parses a command or function define.
     * 
     * Starts consuming AFTER the starting DIRECTIVE Token.
     * 
     * @param directiveOrigin The origin of the directive Token
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void parseDefine(Origin directiveOrigin) throws LexicalError, ParseError
    {
        Command command = Command.fromName(this.expectNameOrFunctionName());
        Set<String> parameterNames = HashSet.newHashSet(3);
        
        head: while (true) {
            Token t = this.tokenizer.next();
            switch (Token.getType(t)) {
                case COMMAND_SYMBOLS:
                    command.addCommandSymbols(t.content);
                    break;
                    
                case DATATYPE:
                    // Parameter
                    Variable.Type type = Variable.Type.fromString(t.content);
                    if (type == null) {
                        throw new ParseError("Unknown parameter type \"" + t.content + "\" at " + t.origin);
                    }
                    String name = this.expectName();
                    Variable parameter;
                    if (type.hasLength()) {
                        int[] length = this.expectLengthRange("Parameter " + t.content);
                        if (type.length != Variable.Type.Length.RANGE && length[0] != length[1]) {
                            throw new ParseError(
                                "Cannot use length range for " + type
                                + " type parameter " + name + " at " + t.origin
                            );
                        }
                        if (length[0] > length[1]) {
                            throw new ParseError(
                                "Minimum length (" + length[0] + ") must not be greater than maximum length ("
                                + length[1] + ") at " + t.origin
                            );
                        }
                        
                        parameter = new Variable(type, name, length[0], length[1]);
                    } else {
                        parameter = new Variable(type, name);
                    }
                    if (type.supportsGroup && this.peekedIsType(Token.Type.DIRECTIVE)) {
                        t = this.tokenizer.next();
                        if (!t.content.equals(".group")) {
                            throw new ParseError(
                                "Unexpected " + t.content + " directive, expected .group at " + t.origin
                            );
                        }
                        parameter.setGroup(this.expectName());
                    }
                    command.addParameter(parameter);
                    if (!parameterNames.add(name)) {
                        throw new ParseError(
                            "Cannot declare parameter " + name + " more than once at " + directiveOrigin
                        );
                    }
                    break;
                    
                case OPENING_BRACES:
                    break head;
                    
                case EOF:
                    throw new ParseError(
                        "Unexpected end of file, expected implementation at " + directiveOrigin
                    );
                default:
                    throw new ParseError("Unexpected " + t.type + " at " + t.origin);
            }
        }
        if (this.ast.commandExists(command.getIdentity())) {
            throw new ParseError(
                "Cannot declare command " + command.getReadableIdentity() + " more than once at " + directiveOrigin
            );
        }
        
        command.setInterpretable(this.parseImplementation(command.getParameters()));
        
        this.expectStatementSeparator();
        this.ast.addCommand(command);
    }
    
    /**
     * Parses a command or function implementation.
     * 
     * Starts AFTER the implementation block's OPENING_BRACES. Stops directly
     * after consuming CLOSING_BRACES.
     * 
     * @param parameters The command parameters. May be null
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private Implementation parseImplementation(List<Variable> parameters) throws LexicalError, ParseError
    {
        Implementation implementation = new Implementation(parameters);
        Tokenizer.Mode surroundingMode = this.tokenizer.getMode();
        
        this.skipStatementSeparators();
        
        body: while (true) {
            Token t = this.tokenizer.next();
            switch (Token.getType(t)) {
                case DIRECTIVE:
                    switch (t.content) {
                        case ".variable":
                        case ".var":
                            String name = this.expectName();
                            if (implementation.variableExists(name)) {
                                throw new ParseError(
                                    "Cannot declare local variable " + name + " more than once at " + t.origin
                                );
                            }
                            
                            // Custom length parsing (to support length register)
                            this.expect(Token.Type.BIT_LENGTH);
                            this.tokenizer.setMode(Tokenizer.Mode.LENGTH);
                            
                            if (this.peekedIsType(Token.Type.NAME)) {
                                // '' varName
                                Origin origin = Token.getOrigin(this.tokenizer.peek());
                                VariableLike lengthRegister = this.expectNumericalVariableLike(
                                    implementation,
                                    "length"
                                );
                                
                                implementation.addVariable(new Variable(
                                    Variable.Type.LOCAL_VARIABLE,
                                    name,
                                    lengthRegister
                                ));
                                
                            } else {
                                implementation.addVariable(new Variable(
                                    Variable.Type.LOCAL_VARIABLE,
                                    name,
                                    this.parseAdvancedLengthFormat("Local variable")
                                ));
                            }
                            
                            this.tokenizer.setMode(surroundingMode);
                            this.expectStatementSeparator();
                            break;
                        default:
                            throw new ParseError(
                                "Unexpected directive " + t.content + " at " + t.origin
                            );
                    }
                    break;
                    
                case NAME:
                case FUNCTION_NAME:
                    try {
                        this.tokenizer.setMode(Tokenizer.Mode.MAIN);
                        implementation.add(this.parseInvocation(t.content, implementation));
                        this.tokenizer.setMode(surroundingMode);
                    } catch (ConstraintException e) {
                        throw new ParseError(
                            "Maximum program size exceeded in implementation at " + t.origin
                            + ". " + e.getMessage()
                        );
                    }
                    break;
                    
                // TODO labels
            
                case CLOSING_BRACES:
                    break body;
                    
                case EOF:
                    throw new ParseError(
                        "Unexpected end of file, expected " + Token.Type.CLOSING_BRACES
                    );
                default:
                    throw new ParseError(
                        "Unexpected " + t.type + ", expected " + Token.Type.CLOSING_BRACES
                );
                    
            }
            this.skipStatementSeparators();
        }
        
        return implementation;
    }
    
    /**
     * Parses one command or function invocation.
     * 
     * Starts after the opening NAME or FUNCTION_NAME Token.
     * 
     * @param name           The name of the invoked command
     * @param implementation The implementation we're currently within. Null if
     *                       within userland code.
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private Invocation parseInvocation(String name, Implementation implementation) throws LexicalError, ParseError
    {
        Invocation invocation = new Invocation(name);
        Tokenizer.Mode surroundingMode = this.tokenizer.getMode();
        while (true) {
            Token t = this.tokenizer.next();
            switch (Token.getType(t)) {
                case COMMAND_SYMBOLS:
                    invocation.addCommandSymbols(t.content);
                    break;
                    
                case NAME:
                    if (this.peekedIsType(Token.Type.BIT_POSITION)) {
                        
                        // BITWISE POSITIONAL ACCESS
                        this.tokenizer.next();
                        if (implementation == null) {
                            throw new ParseError(
                                "Bitwise access not allowed at " + t.origin
                            );
                        }
                        
                        VariableLike register = this.resolveVariableLike(t.content, implementation, t.origin);
                        if (!register.isNumeric()) {
                            throw new ParseError(
                                "Parameter " + register.name
                                + " cannot be accessed bitwise (it is not a numeric value) at " + t.origin
                            );
                        }
                        
                        this.tokenizer.setMode(Tokenizer.Mode.POSITION);
                        RegisterArgument argument;
                        if (this.peekedIsType(Token.Type.NAME)) {
                            // ' varName ...
                            VariableLike fromPositionRegister = this.expectNumericalVariableLike(
                                implementation,
                                "position"
                            );
                            
                            if (this.peekedIsType(Token.Type.POSITION_RANGE)) {
                                this.tokenizer.next();
                                
                                if (this.peekedIsType(Token.Type.NAME)) {
                                    // ' varName : varName
                                    VariableLike toPositionRegister = this.expectNumericalVariableLike(
                                        implementation,
                                        "position"
                                    );
                                    argument = new RegisterArgument(register, fromPositionRegister, toPositionRegister);
                                } else {
                                    // ' varName : number
                                    int toPosition = this.number2PositionInt(this.expect(Token.Type.NUMBER));
                                    Constraints.checkPositionWithinRegister(toPosition, register, t.origin);
                                    argument = new RegisterArgument(register, fromPositionRegister, toPosition);
                                }
                            } else {
                                // ' varName
                                argument = new RegisterArgument(register, fromPositionRegister);
                            }
                        } else {
                            // ' number ...
                            int fromPosition = this.number2PositionInt(this.expect(Token.Type.NUMBER));
                            Constraints.checkPositionWithinRegister(fromPosition, register, t.origin);
                            
                            if (this.peekedIsType(Token.Type.POSITION_RANGE)) {
                                this.tokenizer.next();
                                
                                if (this.peekedIsType(Token.Type.NAME)) {
                                    // ' number : varName
                                    VariableLike toPositionRegister = this.expectNumericalVariableLike(
                                        implementation,
                                        "position"
                                    );
                                    argument = new RegisterArgument(register, fromPosition, toPositionRegister);
                                } else {
                                    // ' number : number
                                    int toPosition = this.number2PositionInt(this.expect(Token.Type.NUMBER));
                                    Constraints.checkPositionWithinRegister(toPosition, register, t.origin);
                                    argument = new RegisterArgument(register, fromPosition, toPosition);
                                }
                            } else {
                                // ' varName
                                argument = new RegisterArgument(register, fromPosition);
                            }
                        }
                        this.tokenizer.setMode(surroundingMode);
                        
                        invocation.addArgument(argument);
                        break;
                    }
                    
                    // Fall-through
                case LABEL_NAME:
                case NUMBER:
                case STRING:
                    invocation.addArgument(Argument.fromToken(t));
                    break;
                    
                case STATEMENT_SEPARATOR:
                    return invocation;
                    
                default:
                    throw new ParseError("Unexpected " + t.toStringWithOrigin());
            }
        }
    }
    
    
    /**
     * Consumes OPENING_BRACES.
     * 
     * Does a tentative parse.
     * 
     * @return True if OPENING_BRACES where consumed.
     * @throws LexicalError
     */
    private boolean consumeOpeningBraces() throws LexicalError
    {
        if (this.peekedIsType(Token.Type.OPENING_BRACES)) {
            this.tokenizer.next();
            this.skipStatementSeparators();
            return true;
        }
        return false;
    }
    
    /**
     * Skips STATEMENT_SEPARATORS if consuming a multi-line directive block.
     * 
     * @param isMultiLine True: currently consuming a multi-line block
     * @throws LexicalError
     */
    private void skipIfMultiLine(boolean isMultiLine) throws LexicalError
    {
        if (isMultiLine) {
            this.skipStatementSeparators();
        }
    }
    
    /**
     * Expects CLOSING_BRACES if consuming a multi-line directive block.
     * 
     * @param isMultiLine True: currently consuming a multi-line block
     * 
     * @throws LexicalError
     * @throws ParseError   If CLOSING_BRACES are not encountered even though
     *                      expected.
     */
    private void expectClosingBracesIfMultiLine(boolean isMultiLine) throws LexicalError, ParseError
    {
        if (isMultiLine) {
            this.expect(Token.Type.CLOSING_BRACES);
        }
    }
    
    
    /**
     * Checks if the upcoming Token is of given type
     * 
     * @param type
     * @return True if peek()ed Token is of given type
     * @throws LexicalError
     */
    private boolean peekedIsType(Token.Type type) throws LexicalError
    {
        return Token.getType(this.tokenizer.peek()) == type;
    }
    
    /**
     * Expects the given Token Type.
     * 
     * @param type
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered
     */
    private Token expect(Token.Type type) throws LexicalError, ParseError
    {
        return this.expect(type, type + " Token");
    }
    
    /**
     * Expects the given Token Type.
     * 
     * @param type
     * @param expected Used in the error message, to inform about what was
     *                 expected instead of what is indeed encountered.
     * 
     * @return The consumed Token, which has {@code type} as type.
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered
     */
    // TODO Try to use this variant (with expected) in more places, i.e. improve
    //      error messages with this
    private Token expect(Token.Type type, String expected) throws LexicalError, ParseError
    {
        Token t = this.tokenizer.next();
        if (!Token.getType(t).equals(type)) {
            throw new ParseError("Expected " + expected + ", encountered " + t.toStringWithOrigin());
        }
        return t;
    }
    
    /**
     * Shorthand for this.expect(Token.Type.STATEMENT_SEPARATOR);
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void expectStatementSeparator() throws LexicalError, ParseError
    {
        this.expect(Token.Type.STATEMENT_SEPARATOR);
    }
    
    /**
     * Skips all STATEMENT_SEPARATOR Tokens until a different type of Token is
     * encountered.
     * 
     * @throws LexicalError
     */
    private void skipStatementSeparators() throws LexicalError
    {
        while (this.peekedIsType(Token.Type.STATEMENT_SEPARATOR)) {
            this.tokenizer.next();
        }
    }
    
    
    /**
     * Expects a NAME Token.
     * 
     * @return The encountered name.
     * 
     * @throws LexicalError
     * @throws ParseError   if no NAME Token is encountered
     */
    private String expectName() throws LexicalError, ParseError
    {
        return this.expect(Token.Type.NAME).content;
    }
    
    /**
     * Expects a NAME or FUNCTION_NAME Token.
     * 
     * @return The encountered name.
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered
     */
    private String expectNameOrFunctionName() throws LexicalError, ParseError
    {
        if (this.peekedIsType(Token.Type.FUNCTION_NAME)) {
            return this.tokenizer.next().content;
        }
        return this.expectName();
    }
    

    /**
     * Expects a NAME and resolves it to a numerical Register, Parameter, or
     * Local Variable.
     * 
     * I.e. the type of the resolved Variable must be numeric.
     * 
     * @see #expectVariableLike()
     * @see #resolveVariableLike()
     * 
     * @param implementation The implementation we're currently within. Null if
     *                       within userland code.
     * @param purpose        Used in the error message
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError   if no NAME Token is encountered, or the encountered
     *                      name cannot be resolved to an existing numerical
     *                      Variable or Register.
     */
    private VariableLike expectNumericalVariableLike(
        Implementation implementation,
        String purpose
    ) throws LexicalError, ParseError {
        Origin origin = Token.getOrigin(this.tokenizer.peek());
        VariableLike variable = this.expectVariableLike(implementation);
        
        if (!variable.isNumeric()) {
            throw new ParseError(
                "Parameter " + variable.name
                + " cannot be used as " + purpose + " (it doesn't contain a numeric value) at "
                + origin
            );
        }
        return variable;
    }
    
    /**
     * Expects a NAME and resolves it to a Register, Parameter, or Local Variable.
     * 
     * @see #resolveVariableLike()
     * 
     * @param implementation The implementation we're currently within. Null if
     *                       within userland code.
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError   if no NAME Token is encountered, or the encountered
     *                      name cannot be resolved to an existing Variable or
     *                      Register.
     */
    private VariableLike expectVariableLike(Implementation implementation) throws LexicalError, ParseError
    {
        Origin origin = Token.getOrigin(this.tokenizer.peek());
        String name = this.expectName();
        
        return this.resolveVariableLike(name, implementation, origin);
    }
    
    /**
     * Resolves given name to Register, Parameter, or Local Variable.<br>
     * 
     * This prefers Parameters and Local Variables over Registers, i.e. a
     * locally-scoped variable can hide a globally-scoped register.
     * 
     * @param name           Name to resolve
     * @param implementation The implementation we're currently within. Null if
     *                       within userland code.
     * @param origin         Used for error message
     * 
     * @return
     * 
     * @throws ParseError if no Variable or Register with given name exists.
     */
    private VariableLike resolveVariableLike(
        String name,
        Implementation implementation,
        Origin origin
    ) throws ParseError {
        if (implementation != null && implementation.variableExists(name)) {
            return implementation.getVariable(name);
        } else if (this.ast.registerExists(name)) {
            return this.ast.getRegister(name);
        } else {
            throw new ParseError(
                "Unknown register or variable " + name + " at " + origin
            );
        }
    }
    
    
    /**
     * Expects an exact length definition. I.e. a BIT_LENGTH Token followed by a
     * NUMBER Token or a "max"/"maxu" entity.
     * 
     * @param elementName Used in the error message. May be null.
     * 
     * @return The length as an integer
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered (incl. a
     *                      number that is not a valid length)
     */
    private int expectLength(String elementName) throws LexicalError, ParseError
    {
        this.expect(Token.Type.BIT_LENGTH);
        
        Tokenizer.Mode oldMode = this.tokenizer.getMode();
        this.tokenizer.setMode(Tokenizer.Mode.LENGTH);
        
        int result = this.parseAdvancedLengthFormat(elementName);
        
        this.tokenizer.setMode(oldMode);
        return result;
    }
    
    /**
     * Expects an exact or range length definition. I.e. a BIT_LENGTH token
     * followed by a valid combination of NUMBER, "max"/"maxu" entities, and
     * some LENGTH_* Tokens.
     * 
     * @param elementName Used in the error message. May be null.
     * 
     * @return [minLength, maxLength]
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered (incl. a
     *                      number that is not a valid length)
     */
    private int[] expectLengthRange(String elementName) throws LexicalError, ParseError
    {
        this.expect(Token.Type.BIT_LENGTH);
        
        Tokenizer.Mode oldMode = this.tokenizer.getMode();
        this.tokenizer.setMode(Tokenizer.Mode.LENGTH);
        
        int[] result = {-1, -1};
        
        switch (Token.getType(this.tokenizer.peek())) {
            case LENGTH_GREATER_THAN_OR_EQUALS:
                // '' >= number
                this.tokenizer.next();
                result[0] = this.parseAdvancedLengthFormat(elementName);
                result[1] = Constraints.MAX_LENGTH;
                break;
                
            case LENGTH_LESS_THAN_OR_EQUALS:
                // '' <= number
                this.tokenizer.next();
                result[0] = Constraints.MIN_LENGTH;
                result[1] = this.parseAdvancedLengthFormat(elementName);
                break;
                
            case NUMBER:
                // '' number
                result[0] = result[1] = this.parseAdvancedLengthFormat(elementName);
                if (this.peekedIsType(Token.Type.LENGTH_RANGE)) {
                    // '' number .. number
                    this.tokenizer.next();
                    result[1] = this.parseAdvancedLengthFormat(elementName);
                }
                // Note: The Token constructed by peek() above may not be
                //       consumed via next() before we reach the end of this
                //       method where the Tokenizer mode is changed - upon the
                //       next peek() or next() after the mode change still the
                //       same Token is provided; theoretically the different
                //       modes could lead to different Token types, thus the
                //       wrong type being provided in such a case.
                //       However, as long as the following conditions hold true,
                //       everything is working fine:
                //       - This method is only used for parameters in .define
                //         directives,
                //       - where LABELS or LABEL_NAMES cannot occur,
                //       - and ".." are not valid COMMAND_SYMBOLS.
                
                break;
            
            case EOF:
                // TODO include origin somehow (so we know which file it is
                //      - check other end-of-file errors as well
                throw new ParseError("Unexpected end of file, expected " + Token.Type.NUMBER);
            
            default:
                throw new ParseError("Unexpected " + this.tokenizer.peek().toStringWithOrigin());
        }
        
        this.tokenizer.setMode(oldMode);
        return result;
    }
    
    /**
     * Parses a single length value, which may be a NUMBER or a "max" or "maxu"
     * entity.<br>
     * 
     * Tokenizer mode must be LENGTH.
     * 
     * @param elementName Used in the error message. May be null.
     * 
     * @return length in bits
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private int parseAdvancedLengthFormat(String elementName) throws LexicalError, ParseError
    {
        switch (Token.getType(this.tokenizer.peek())) {
            case LENGTH_MAX:
                // max<number>
                this.tokenizer.next();
                return this.number2fittingLengthInt(
                    this.expect(Token.Type.NUMBER),
                    false,
                    elementName
                );
            case LENGTH_MAXU:
                // maxu<number> (unsigned)
                this.tokenizer.next();
                return this.number2fittingLengthInt(
                    this.expect(Token.Type.NUMBER),
                    true,
                    elementName
                );
                    
            default:
                // <number>
                return this.number2LengthInt(
                    this.expect(Token.Type.NUMBER),
                    elementName
                );
        }
    }
    
    /**
     * Transforms a NUMBER Token to a length integer.
     * 
     * @param token
     * @param elementName Used in the error message. May be null
     * 
     * @return
     * 
     * @throws ParseError if given number is not a valid length
     */
    private int number2LengthInt(Token token, String elementName) throws ParseError
    {
        int length;
        try {
            length = Token.number2Int(token);
        } catch (ConstraintException e) {
            throw Constraints.lengthError(token.content, elementName, Token.getOrigin(token));
        }
        Constraints.checkLength(length, elementName, Token.getOrigin(token));
        
        return length;
    }
    
    /**
     * Transforms a NUMBER Token to a fitting length integer.
     * 
     * I.e. this figures how many length bits a variable (or register) would
     * need to have to accomodate the given number.
     * 
     * @param token       A NUMBER Token with a positive number
     * @param unsigned    False: Length is chosen to accomodate negative numbers
     *                    as well (Twos Complement) ("max" behavior). (True:
     *                    "maxu" behavior)
     * @param elementName Used in a length error message. May be null
     * 
     * @return Smallest length big enough to fit the given number as a value.
     * 
     * @throws ParseError If a negative number is given, or resulting length is
     *                    too big
     */
    private int number2fittingLengthInt(Token token, boolean unsigned, String elementName) throws ParseError
    {
        BigInteger number = Token.number2BigInteger(token);
        if (number.signum() <= 0) {
            throw new ParseError(
                "Only positive numbers supported, given " + number + " at " + Token.getOrigin(token)
            );
        }
        int length = number.bitLength();
        if (!unsigned) {
            // sign bit
            length++;
        }

        Constraints.checkLength(length, elementName, Token.getOrigin(token));
        
        return length;
    }

    
    /**
     * Transforms a NUMBER Token to a position integer.
     * 
     * @param token
     * @return
     * @throws ParseError if given number is not a valid position
     */
    private int number2PositionInt(Token token) throws ParseError
    {
        int position;
        try {
            position = Token.number2Int(token);
        } catch (ConstraintException e) {
            throw Constraints.positionError(token.content, Token.getOrigin(token));
        }
        Constraints.checkPosition(position, Token.getOrigin(token));
        
        return position;
    }
    
    
    /**
     * Expects specific COMMAND_SYMBOLS.
     * 
     * @param expectedSymbols Specific expected command symbols, as defined in
     *                        {@link Tokenizer}.COMMAND_SYMBOLS.
     * 
     * @throws LexicalError
     * @throws ParseError   if the expected symbols are not encountered
     */
    private void expectCommandSymbols(String expectedSymbols) throws LexicalError, ParseError
    {
        String consumed = this.expect(Token.Type.COMMAND_SYMBOLS, "\"" + expectedSymbols + "\"").content;
        
        // TODO support command symbols spanning several tokens - but for now it
        //      is not an issue as this is only used in one place, with an
        //      expectedSymbols that is one char long.
        if (!expectedSymbols.equals(consumed)) {
            throw new ParseError("Expected \"" + expectedSymbols + "\" symbols, is \"" + consumed + "\"");
        }
    }
    
    
    /**
     * Resolves all invocations that are part of a (command or function)
     * implementation. I.e. this is the deferred invocation resolution for
     * implementations (whereas invocations within userland program are
     * resolved immediately).<br>
     * 
     * Call this once after all parsing is done.
     * 
     * @throws ParseError
     */
    public void resolveImplementationInvocations() throws ParseError
    {
        for (Command command : this.ast.getCommands()) {
            if (command.getInterpretable() instanceof Implementation)
            this.resolveWithinImplementation((Implementation)command.getInterpretable());
        }
        
        for (Register register : this.ast.getRegisters()) {
            if (register instanceof VirtualRegister) {
                VirtualRegister vr = (VirtualRegister)register;
                this.resolveWithinImplementation(vr.getGetterImplementation());
                this.resolveWithinImplementation(vr.getSetterImplementation());
            }
        }
    }
    
    /**
     * Used by {@link #resolveImplementationInvocations()}.
     * 
     * @param implementation
     * @throws ParseError
     */
    private void resolveWithinImplementation(Implementation implementation) throws ParseError
    {
        for (Invocation invocation : implementation) {
            try {
                invocation.resolve(this.ast, implementation);
            } catch (ConstraintException e) {
                // TODO include origin (somehow?)
                throw new ParseError(e.getMessage());
            }
        }
    }
}
