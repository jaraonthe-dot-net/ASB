package net.jaraonthe.java.asb.parse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.command.Function;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.ast.variable.Variable;
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
    //      - negative NUMBER => subtract positive number (in expression)
    
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
                    Invocation invocation = this.parseInvocation(t.content);
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
                    // TODO
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
                // TODO add do docs: After closing braces, either newline or ; must follow for separation
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
        } else {
            // .register_virtual (...) ...sub-directives
            this.parseGroups(register, isMultiLine);
            
            Token t = this.tokenizer.peek();
            if (Token.getType(t) == Token.Type.DIRECTIVE && t.content.equals(".store")) {
                this.tokenizer.next();
                VirtualRegister vr = (VirtualRegister)register;
                vr.setStore(this.expectLength("register " + name + " store"));
                
                this.skipIfMultiLine(isMultiLine);
                this.parseGroups(register, isMultiLine);
            }
            // TODO .get and .set
        }
        
        this.expectClosingBracesIfMultiLine(isMultiLine);
        this.expectStatementSeparator();
        
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
                    if (type.hasLength) {
                        parameter = new Variable(type, name, this.expectLength("Parameter " + t.content));
                    } else {
                        parameter = new Variable(type, name);
                    }
                    if (type.supportsGroup && Token.getType(this.tokenizer.peek()) == Token.Type.DIRECTIVE) {
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
        
        command.setImplementation(this.parseImplementation(command.getParameters()));
        this.ast.addCommand(command);
    }
    
    /**
     * Parses a command or function implementation.
     * 
     * Starts AFTER the implementation block's OPENING_BRACES.
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
        
        this.skipStatementSeparators();
        
        body: while (true) {
            Token t = this.tokenizer.next();
            switch (Token.getType(t)) {
                case DIRECTIVE:
                    switch (t.content) {
                        case ".variable":
                        case ".var":
                            implementation.addVariable(new Variable(
                                Variable.Type.LOCAL_VARIABLE,
                                this.expectName(),
                                this.expectLength("Variable")
                            ));
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
                        implementation.add(this.parseInvocation(t.content));
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
        
        this.expectStatementSeparator();    
        return implementation;
    }
    
    /**
     * Parses one command or function invocation.
     * 
     * Starts after the opening NAME or FUNCTION_NAME Token.
     * 
     * @param name The name of the invoked command
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private Invocation parseInvocation(String name) throws LexicalError, ParseError
    {
        Invocation invocation = new Invocation(name);
        while (true) {
            Token t = this.tokenizer.next();
            switch (Token.getType(t)) {
                case COMMAND_SYMBOLS:
                    invocation.addCommandSymbols(t.content);
                    break;
                case NAME:
                case LABEL_NAME:
                case NUMBER:
                case STRING:
                    invocation.addArgument(t);
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
        if (Token.getType(this.tokenizer.peek()).equals(Token.Type.OPENING_BRACES)) {
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
        while (Token.getType(this.tokenizer.peek()) == Token.Type.STATEMENT_SEPARATOR) {
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
        if (Token.getType(this.tokenizer.peek()) == Token.Type.FUNCTION_NAME) {
            return this.tokenizer.next().content;
        }
        return this.expectName();
    }
    
    /**
     * Expects a length definition. I.e. a BIT_LENGTH Token followed by a NUMBER
     * Token.
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
        
        return this.number2LengthInt(
            this.expect(Token.Type.NUMBER),
            elementName
        );
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
            Implementation implementation = command.getImplementation();
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
}
