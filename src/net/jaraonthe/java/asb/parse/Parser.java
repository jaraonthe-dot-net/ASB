package net.jaraonthe.java.asb.parse;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jaraonthe.java.asb.ast.AST;
import net.jaraonthe.java.asb.ast.command.Command;
import net.jaraonthe.java.asb.ast.command.Implementation;
import net.jaraonthe.java.asb.ast.invocation.CommandInvocation;
import net.jaraonthe.java.asb.ast.invocation.ImmediateArgument;
import net.jaraonthe.java.asb.ast.invocation.Invocation;
import net.jaraonthe.java.asb.ast.invocation.LabelArgument;
import net.jaraonthe.java.asb.ast.invocation.RawArgument;
import net.jaraonthe.java.asb.ast.invocation.StringArgument;
import net.jaraonthe.java.asb.ast.invocation.VariableArgument;
import net.jaraonthe.java.asb.ast.variable.LocalVariable;
import net.jaraonthe.java.asb.ast.variable.Parameter;
import net.jaraonthe.java.asb.ast.variable.Register;
import net.jaraonthe.java.asb.ast.variable.RegisterAlias;
import net.jaraonthe.java.asb.ast.variable.RegisterLike;
import net.jaraonthe.java.asb.ast.variable.Variable;
import net.jaraonthe.java.asb.ast.variable.VirtualRegister;
import net.jaraonthe.java.asb.built_in.BuiltInFunction;
import net.jaraonthe.java.asb.exception.ConstraintException;
import net.jaraonthe.java.asb.exception.LexicalError;
import net.jaraonthe.java.asb.exception.ParseError;
import net.jaraonthe.java.asb.interpret.Interpreter;

/**
 * Parses ASB source code into AST.<br>
 * 
 * Takes care of all semantic rules and constraints. Applies all necessary
 * transformations. The resulting AST can be executed directly (see {@link
 * Interpreter#interpret()}).<br>
 * 
 * Each Parser instance parses exactly one source file. Use {@link #parse()}
 * to run the parsing procedure in its entirety.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Parser
{
    /**
     * The AST that will be filled by this Parser.
     */
    private final AST ast;
    
    /**
     * This Parser's Tokenizer (through which both are coupled to one Sourcefile).
     */
    private final Tokenizer tokenizer;
    
    
    /**
     * Executes the entire parsing procedure.
     * 
     * This includes parsing all files, resolving all variables and invocations
     * and other possible post-parsing checks.
     * 
     * @param filePaths The files represented by these paths are parsed in the
     *                  given order.
     * 
     * @return A complete and correct AST, which is ready to be interpreted.
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    public static AST parse(List<String> filePaths) throws LexicalError, ParseError
    {
        AST ast = new AST();
        BuiltInFunction.initBuiltInFunctions(ast);
        
        for (String filePath : filePaths) {
            SourceFile file;
            try {
                file = new SourceFile(filePath);
            } catch (IOException e) {
                throw new ParseError(
                    "Cannot open file " + filePath + " for parsing"
                );
            }
            Parser parser = new Parser(file, ast);
            parser.run();
        }
        
        Parser.resolveImplementationInvocations(ast);
        Parser.resolveLabelNamesInUserland(ast);
        
        return ast;
    }
    
    
    /**
     * @param file The file that this Parser instance works on
     * @param ast
     */
    protected Parser(SourceFile file, AST ast)
    {
        this.tokenizer = new Tokenizer(file);
        this.ast       = ast;
    }
    
    /**
     * Runs this Parser, i.e. parses the configured file.
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void run() throws LexicalError, ParseError
    {
        this.ast.addParsedFilePath(this.tokenizer.file.filePath);
        Token t;
        while ((t = this.tokenizer.next()) != null) {
            switch (t.type) {
                case DIRECTIVE:
                    this.parseDirective(t);
                    break;
                    
                case NAME:
                case FUNCTION_NAME:
                    CommandInvocation invocation = this.parseInvocation(t.content, null, t.origin);
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
                    if (this.ast.labelExists(t.content)) {
                        throw new ParseError("Label " + t.content + " already exists at " + t.origin);
                    }
                    this.ast.addLabel(t.content);
                    break;
                    
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
        boolean isIncludeOnce = false;
        switch (directive.content) {
            case ".include_once":
                isIncludeOnce = true;
                // Fall-through
            case ".include":
                t = this.expect(Token.Type.STRING, "file path");
                this.expectStatementSeparator();
                
                // Resolve given path relatively against directory of current file
                Path filePath = this.tokenizer.file.filePath.getParent().resolve(t.content);
                SourceFile file;
                try {
                    if (isIncludeOnce) {
                        filePath = SourceFile.normalizeFilePath(filePath);
                        if (this.ast.alreadyParsedFilePath(filePath)) {
                            // .include_once already parsed file
                            break;
                        }
                    }
                    
                    file = new SourceFile(filePath);
                } catch (IOException e) {
                    throw new ParseError(
                        "Cannot open file " + filePath + " for parsing, included at " + directive.origin
                    );
                }
                
                new Parser(file, this.ast).run();
                
                break;
            case ".memory":
                if (this.ast.hasProgram()) {
                    throw new ParseError(
                        "Cannot configure memory after first command at" + directive.origin
                    );
                }
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
                this.expectStatementSeparator();
                break;

            case ".program_counter":
            case ".pc":
                if (this.ast.hasProgram()) {
                    throw new ParseError(
                        "Cannot configure program counter after first command at" + directive.origin
                    );
                }
                // TODO Issue: Even though no command invocation has been
                //      encountered yet, the pc length may already have been
                //      used for a /label parameter in a command .define! I.e.
                //      that commands label param length is incorrect! Not sure
                //      what that will cause, but it's not good.
                //      Let's prevent changing of pc after the first /label
                //      parameter has been defined.
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

            case ".virtual_register":
            case ".virtual_reg":
            case ".virt_register":
            case ".virt_reg":
            case ".register_virtual":
            case ".register_virt":
            case ".reg_virtual":
            case ".reg_virt":
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
     * .register_alias, or a .virtual_register directive is being parsed.
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
            // .virtual_register <name> ''<length> (...)
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
            
            // .virtual_register (...) ...sub-directives
            VirtualRegister vr = (VirtualRegister)register;
            Token t;
            ArrayList<Parameter> parameters;
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
                        this.expect(Token.Type.OPENING_BRACES, "opening braces (for .get implementation)");
                        parameters = new ArrayList<>(1);
                        parameters.add(new Parameter(Parameter.Type.REGISTER, "out", register.getLength()));
                        if (vr.hasStore()) {
                            parameters.add(new Parameter(Parameter.Type.REGISTER, "store", vr.getStoreLength()));
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
                        this.expect(Token.Type.OPENING_BRACES, "opening braces (for .set implementation)");
                        parameters = new ArrayList<>(1);
                        parameters.add(new Parameter(Parameter.Type.REGISTER, "in", register.getLength()));
                        if (vr.hasStore()) {
                            parameters.add(new Parameter(Parameter.Type.REGISTER, "store", vr.getStoreLength()));
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
     * Parses groups sub-directives for a register or local variable definition.<br>
     * 
     * Does a tentative parse, stopping as soon as anything other than a .group
     * sub-directive is encountered.
     * 
     * @param registerLike The register or local variable being worked on
     * @param isMultiLine  True if directive is using a multi-line block
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void parseGroups(RegisterLike register, boolean isMultiLine) throws LexicalError, ParseError
    {
        Token t;
        while (Token.getType(t = this.tokenizer.peek()) == Token.Type.DIRECTIVE) {
            switch (t.content) {
                case ".group":
                    this.tokenizer.next();
                    String name = this.expectName();
                    if (register.hasGroup(name)) {
                        throw new ParseError(
                            "Cannot declare group " + name + " more than once for "
                            + (register instanceof Register ? "register" : "local variable")
                            +" " + register.name + " at " + t.origin
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
                    Parameter.Type type = Parameter.Type.fromString(t.content);
                    if (type == null) {
                        throw new ParseError("Unknown parameter type \"" + t.content + "\" at " + t.origin);
                    }
                    String name = this.expectName();
                    Parameter parameter;
                    if (type.hasLength()) {
                        int[] length;
                        if (type == Parameter.Type.LABEL) {
                            // Use pc length for label types
                            length = new int[]{this.ast.getPcLength(), this.ast.getPcLength()};
                        } else {
                            length = this.expectLengthRange("Parameter " + t.content);
                        }
                        if (type.length != Parameter.Type.Length.RANGE && length[0] != length[1]) {
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
                        
                        parameter = new Parameter(type, name, length[0], length[1]);
                    } else {
                        parameter = new Parameter(type, name);
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
                    
                default:
                    throw new ParseError("Unexpected " + t.type + " Token at " + t.origin);
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
    private Implementation parseImplementation(List<Parameter> parameters) throws LexicalError, ParseError
    {
        Implementation implementation = new Implementation(parameters);
        Tokenizer.Mode surroundingMode = this.tokenizer.getMode();
        
        this.skipStatementSeparators();
        
        body: while (true) {
            Token t = this.tokenizer.next();
            try {
                switch (Token.getType(t)) {
                    case DIRECTIVE:
                        switch (t.content) {
                            case ".variable":
                            case ".var":
                                String name = this.expectName();
                                if (implementation.variableExists(name)) {
                                    throw new ParseError(
                                        "Cannot declare local variable " + name
                                        + " more than once at " + t.origin
                                    );
                                }
                                LocalVariable variable;
                                
                                // Custom length parsing (to support length variable)
                                this.expect(Token.Type.BIT_LENGTH, "'' (to give bit length)");
                                this.tokenizer.setMode(Tokenizer.Mode.LENGTH);
                                if (this.peekedIsType(Token.Type.NAME)) {
                                    // '' varName
                                    Variable lengthVariable = this.expectNumericVariable(
                                        implementation,
                                        "length"
                                    );
                                    variable = new LocalVariable(
                                        name,
                                        lengthVariable
                                    );
                                    
                                } else {
                                    variable = new LocalVariable(
                                        name,
                                        this.parseAdvancedLengthFormat("Local variable")
                                    );
                                }
                                this.tokenizer.setMode(surroundingMode);
                                
                                boolean isMultiLine = this.consumeOpeningBraces();
                                this.parseGroups(variable, isMultiLine);
                                
                                this.expectClosingBracesIfMultiLine(isMultiLine);
                                this.expectStatementSeparator();
                                
                                // Here we don't use the full Origin (from
                                // .variable until end of length definition),
                                // but only
                                // the origin of the .variable directive Token.
                                // For what we intend to do with this data this
                                // should be okay.
                                implementation.addLocalVariable(variable, t.origin);
                                break;
                            default:
                                throw new ParseError(
                                    "Unexpected directive " + t.content + " at " + t.origin
                                );
                        }
                        break;
                        
                    case NAME:
                    case FUNCTION_NAME:
                            this.tokenizer.setMode(Tokenizer.Mode.MAIN);
                            implementation.add(this.parseInvocation(t.content, implementation, t.origin));
                            this.tokenizer.setMode(surroundingMode);
                        break;
                        
                    case LABEL:
                        if (implementation.labelExists(t.content)) {
                            throw new ParseError("Local label " + t.content + " already exists at " + t.origin);
                        }
                        implementation.addLabel(t.content);
                        break;
                
                    case CLOSING_BRACES:
                        break body;
                        
                    case EOF:
                        throw new ParseError(
                            "Unexpected end of file, expected closing braces in "
                            + this.tokenizer.file.filePath.toString()
                        );
                    default:
                        throw new ParseError(
                            "Unexpected " + t.type + ", expected closing braces at " + t.origin
                        );
                        
                }
            } catch (ConstraintException e) {
                // From implementation.addLocalVariable() or implementation.add()
                throw new ParseError(
                    "Maximum program size exceeded in implementation at " + Token.getOrigin(t)
                    + ". " + e.getMessage()
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
     * @param openingOrigin  The origin of the starting NAME or FUNCTION_NAME
     *                       Token.
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private CommandInvocation parseInvocation(
        String name,
        Implementation implementation,
        Origin openingOrigin
    ) throws LexicalError, ParseError {
        CommandInvocation invocation   = new CommandInvocation(name);
        Tokenizer.Mode surroundingMode = this.tokenizer.getMode();
        Origin previousOrigin          = openingOrigin;
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
                        
                        Variable variable = this.resolveVariable(
                            t.content,
                            implementation,
                            t.origin,
                            true
                        );
                        if (!variable.isNumeric()) {
                            throw new ParseError(
                                "Parameter " + variable.name
                                + " cannot be accessed bitwise (it is not a numeric value) at " + t.origin
                            );
                        }
                        
                        this.tokenizer.setMode(Tokenizer.Mode.POSITION);
                        VariableArgument argument;
                        if (this.peekedIsType(Token.Type.NAME)) {
                            // ' varName ...
                            Variable fromPositionVariable = this.expectNumericVariable(
                                implementation,
                                "position"
                            );
                            
                            if (this.peekedIsType(Token.Type.POSITION_RANGE)) {
                                this.tokenizer.next();
                                
                                if (this.peekedIsType(Token.Type.NAME)) {
                                    // ' varName : varName
                                    Variable toPositionVariable = this.expectNumericVariable(
                                        implementation,
                                        "position"
                                    );
                                    argument = new VariableArgument(variable, fromPositionVariable, toPositionVariable);
                                } else {
                                    // ' varName : number
                                    int toPosition = this.number2PositionInt(this.expect(
                                        Token.Type.NUMBER,
                                        "a number (as end of bitwise access range)"
                                    ));
                                    Constraints.checkPositionWithinVariable(toPosition, variable, t.origin);
                                    argument = new VariableArgument(variable, fromPositionVariable, toPosition);
                                }
                            } else {
                                // ' varName
                                argument = new VariableArgument(variable, fromPositionVariable);
                            }
                        } else {
                            // ' number ...
                            int fromPosition = this.number2PositionInt(this.expect(
                                Token.Type.NUMBER,
                                "a number (as beginning of bitwise access range)"
                            ));
                            Constraints.checkPositionWithinVariable(fromPosition, variable, t.origin);
                            
                            if (this.peekedIsType(Token.Type.POSITION_RANGE)) {
                                this.tokenizer.next();
                                
                                if (this.peekedIsType(Token.Type.NAME)) {
                                    // ' number : varName
                                    Variable toPositionVariable = this.expectNumericVariable(
                                        implementation,
                                        "position"
                                    );
                                    argument = new VariableArgument(variable, fromPosition, toPositionVariable);
                                } else {
                                    // ' number : number
                                    int toPosition = this.number2PositionInt(this.expect(
                                        Token.Type.NUMBER,
                                        "a number (as end of bitwise access range)"
                                    ));
                                    Constraints.checkPositionWithinVariable(toPosition, variable, t.origin);
                                    argument = new VariableArgument(variable, fromPosition, toPosition);
                                }
                            } else {
                                // ' varName
                                argument = new VariableArgument(variable, fromPosition);
                            }
                        }
                        this.tokenizer.setMode(surroundingMode);
                        
                        invocation.addArgument(argument);
                        break;
                    }
                    
                    Variable potentialVariable = this.resolveVariable(
                        t.content,
                        implementation,
                        t.origin,
                        false
                    );
                    if (potentialVariable != null && potentialVariable.isNumeric()) {
                        // May be a variable or label (depending on effective command)
                        invocation.addArgument(new RawArgument(t.content, potentialVariable));
                        break;
                    }
                    
                    // Fall-through (it can only be a label)
                case LABEL_NAME:
                case DIRECTIVE: // Fluke Token - is indeed label in this context
                    invocation.addArgument(new LabelArgument(t.content));
                    break;
                    
                case NUMBER:
                 // immediate
                    invocation.addArgument(new ImmediateArgument(Token.number2BigInteger(t), t.content));
                    break;
                    
                case STRING:
                 // string
                    invocation.addArgument(new StringArgument(t.content));
                    break;
                    
                case STATEMENT_SEPARATOR:
                    invocation.setOrigin(openingOrigin.merge(previousOrigin));
                    return invocation;
                    
                default:
                    throw new ParseError("Unexpected " + t.toStringWithOrigin());
            }
            previousOrigin = t.origin;
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
            this.expect(Token.Type.CLOSING_BRACES, "closing braces for a multi-line directive");
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
     * @param expected Used in the error message, to inform about what was
     *                 expected instead of what is indeed encountered.
     * 
     * @return The consumed Token, which has {@code type} as type.
     * 
     * @throws LexicalError
     * @throws ParseError   if something unexpected is encountered
     */
    private Token expect(Token.Type type, String expected) throws LexicalError, ParseError
    {
        Token t = this.tokenizer.next();
        if (!Token.getType(t).equals(type)) {
            throw new ParseError("Expected " + expected + ", encountered " + t.toStringWithOrigin());
        }
        return t;
    }
    
    /**
     * Shorthand for {@code this.expect(Token.Type.STATEMENT_SEPARATOR)};
     * 
     * @throws LexicalError
     * @throws ParseError
     */
    private void expectStatementSeparator() throws LexicalError, ParseError
    {
        this.expect(Token.Type.STATEMENT_SEPARATOR, "; or newline (statement separator)");
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
        return this.expect(Token.Type.NAME, "a name").content;
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
     * Expects a NAME and resolves it to a numeric Register, Parameter, or
     * Local Variable.
     * 
     * I.e. the type of the resolved Variable must be numeric.
     * 
     * @see #expectVariable()
     * @see #resolveVariable()
     * 
     * @param implementation The implementation we're currently within. Null if
     *                       within userland code.
     * @param purpose        Used in the error message
     * 
     * @return
     * 
     * @throws LexicalError
     * @throws ParseError   if no NAME Token is encountered, or the encountered
     *                      name cannot be resolved to an existing numeric
     *                      Variable or Register.
     */
    private Variable expectNumericVariable(
        Implementation implementation,
        String purpose
    ) throws LexicalError, ParseError {
        Origin origin = Token.getOrigin(this.tokenizer.peek());
        Variable variable = this.expectVariable(implementation);
        
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
     * @see #resolveVariable()
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
    private Variable expectVariable(Implementation implementation) throws LexicalError, ParseError
    {
        Origin origin = Token.getOrigin(this.tokenizer.peek());
        String name = this.expectName();
        
        return this.resolveVariable(name, implementation, origin, true);
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
     * @param throwError     True: Throw error if no Variable or Register with
     *                       given name exists. False: Return null instead.
     * 
     * @return May return null, depending on throwError.
     * 
     * @throws ParseError see throwError
     */
    private Variable resolveVariable(
        String name,
        Implementation implementation,
        Origin origin,
        boolean throwError
    ) throws ParseError {
        if (implementation != null && implementation.variableExists(name)) {
            return implementation.getVariable(name);
        } else if (this.ast.registerExists(name)) {
            return this.ast.getRegister(name);
        } else if (throwError) {
            throw new ParseError(
                "Unknown register or variable " + name + " at " + origin
            );
        }
        return null;
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
        this.expect(Token.Type.BIT_LENGTH, "'' (to give bit length)");
        
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
        this.expect(Token.Type.BIT_LENGTH, "'' (to give bit length)");
        
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

            case LENGTH_MAX:
            case LENGTH_MAXU:
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
                    this.expect(Token.Type.NUMBER, "a number (for max)"),
                    false,
                    elementName
                );
            case LENGTH_MAXU:
                // maxu<number> (unsigned)
                this.tokenizer.next();
                return this.number2fittingLengthInt(
                    this.expect(Token.Type.NUMBER, "a number (for maxu)"),
                    true,
                    elementName
                );
                    
            default:
                // <number>
                return this.number2LengthInt(
                    this.expect(Token.Type.NUMBER, "a number (as length)"),
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
     * need to have to accommodate the given number.
     * 
     * @param token       A NUMBER Token with a positive number
     * @param unsigned    False: Length is chosen to accomodate negative numbers
     *                    as well (two's-complement) ("max" behavior). (True:
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
        Token token = this.expect(Token.Type.COMMAND_SYMBOLS, "\"" + expectedSymbols + "\"");
        String consumed = token.content;
        
        // TODO Consider supporting command symbols spanning several tokens -
        //      but for now it is not an issue as this is only used in one
        //      place, with an expectedSymbols that is one char long.
        if (!expectedSymbols.equals(consumed)) {
            throw new ParseError(
                "Expected \"" + expectedSymbols + "\" symbols, is \"" + consumed + "\" at " + token.origin
            );
        }
    }
    
    
    /**
     * Resolves all invocations that are part of a (command or function)
     * implementation. I.e. this is the deferred invocation resolution for
     * implementations (whereas invocations within userland program are
     * resolved immediately).<br>
     * 
     * Takes care of {@link Invocation#resolve() resolve()} and
     * {@link Invocation#resolveLabelNames() resolveLabelNames()}.<br>
     * 
     * This is called once after all parsing is done.
     * 
     * @param ast
     * @throws ParseError
     */
    private static void resolveImplementationInvocations(AST ast) throws ParseError
    {
        for (Command command : ast.getCommands()) {
            if (command.getInterpretable() instanceof Implementation) {
                Parser.resolveWithinImplementation((Implementation)command.getInterpretable(), ast);
            }
        }
        
        for (Register register : ast.getRegisters()) {
            if (register instanceof VirtualRegister) {
                VirtualRegister vr = (VirtualRegister)register;
                Parser.resolveWithinImplementation(vr.getGetterImplementation(), ast);
                Parser.resolveWithinImplementation(vr.getSetterImplementation(), ast);
            }
        }
    }
    
    /**
     * Used by {@link #resolveImplementationInvocations()}.
     * 
     * Takes care of {@link Invocation#resolve() resolve()} and
     * {@link Invocation#resolveLabelNames() resolveLabelNames()}.
     * 
     * @param implementation
     * @param ast
     * 
     * @throws ParseError
     */
    private static void resolveWithinImplementation(Implementation implementation, AST ast) throws ParseError
    {
        for (Invocation invocation : implementation) {
            try {
                invocation.resolve(ast, implementation);
                invocation.resolveLabelNames(ast, implementation);
            } catch (ConstraintException e) {
                throw new ParseError(e.getMessage() + " at " + invocation.getOrigin());
            }
        }
    }
    
    
    /**
     * Resolves label names that are used as arguments within invocations that
     * are part of userland code.<br>
     * 
     * This is called once after all parsing is done.
     * 
     * @param ast
     * @throws ParseError
     */
    private static void resolveLabelNamesInUserland(AST ast) throws ParseError
    {
        for (Invocation invocation : ast.getProgram()) {
            try {
                invocation.resolveLabelNames(ast, null);
            } catch (ConstraintException e) {
                throw new ParseError(e.getMessage() + " at " + invocation.getOrigin());
            }
        }
        
    }
}
