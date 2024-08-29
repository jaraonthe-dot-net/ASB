package net.jaraonthe.java.asb.parse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jaraonthe.java.asb.exception.LexicalError;

/**
 * The Tokenizer aka Lexer aka Scanner. Tokenizes exactly one file as a service
 * to the Parser.<br>
 * 
 * Note that this operates in one of several modes, and it relies on the Parser
 * to set a proper mode. This is necessary because some language constructs
 * are syntactically ambiguous without context. Thus, running this without the
 * Parser may not create the expected result.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Tokenizer
{
    public enum Mode
    {
        /**
         * The default mode for parsing command invocations.
         */
        MAIN,
        
        /**
         * Used for meta code, i.e. once a directive has been encountered.
         */
        META,
        
        /**
         * When parsing a length setting (within META code).
         */
        LENGTH,
        
        /**
         * When parsing a bit position.
         */
        POSITION,
    }
    
    
    private static final Pattern NAME_PATTERN        = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.]*");
    private static final Pattern FULL_NUMBER_PATTERN = Pattern.compile(
        "^(-?[1-9][_0-9]*|0(x|X)_?[0-9A-Fa-f][_0-9A-Fa-f]*|0(b|B)_?[01][_01]*|0[_0-7]*)$"
    );
    private static final Pattern NEGATIVE_NUMBER_PATTERN = Pattern.compile("^-[1-9][_0-9]*");
    private static final Pattern LABEL_PATTERN           = Pattern.compile("^([A-Za-z0-9_.]+):");
    private static final Pattern LABEL_NAME_PATTERN      = Pattern.compile("^[A-Za-z0-9_.]+");
    
    private static final String COMMAND_SYMBOLS           = "!$%&()*+,/<=>?@[]^`{|}~";
    private static final String META_MODE_COMMAND_SYMBOLS = "!$%&()*+,<=>?@[]^`|~";
    
    
    /**
     * The file that's being tokenized.
     */
    public final SourceFile file;
    
    /**
     * The current line in the file (0-based).
     */
    private int currentLine = 0;
    
    /**
     * The current position on the current line (0-based).
     */
    private int currentCol = 0;
    
    /**
     * The content of the currently processed line (caching). If null we have
     * reached the end-of-file.
     */
    private String lineContent;
    
    /**
     * Governs Tokenizer behavior
     */
    private Tokenizer.Mode mode = Mode.MAIN;
    
    /**
     * When {@link #peek()}ing, this contains the next Token (so that it doesn't
     * have to be tokenized more than once).
     */
    private Token nextToken = null;
    
    
    
    /**
     * @param file The file that shall be tokenized
     */
    public Tokenizer(SourceFile file)
    {
        this.file = file;
        if (file.lines.size() > 0) {
            this.lineContent = file.lines.get(0);
        } else {
            this.lineContent = null;
        }
    }
    
    /**
     * Provides the next token.<br>
     * 
     * This advances the internal position in the file, i.e. every invocation
     * returns a different Token.
     * 
     * @return A Token, or null if reached end-of-file.
     * @throws LexicalError
     */
    public Token next() throws LexicalError
    {
        if (this.nextToken != null) {
            Token t = this.nextToken;
            this.nextToken = null;
            
            return t;
        }
        return this.consumeNext();
    }
    
    /**
     * Take a peek at the next Token.<br>
     * 
     * This returns the next Token like {@link #next()}, but the position in the
     * file is not advanced, i.e. the same Token is returned over and over until
     * position is advanced via {@link #next()}.
     * 
     * @return A Token, or null if reached end-of-file.
     * @throws LexicalError
     */
    public Token peek() throws LexicalError
    {
        if (this.nextToken == null) {
            this.nextToken = this.consumeNext();
        }
        return this.nextToken;
    }
    
    /**
     * @return the current Tokenizer mode.
     */
    public Tokenizer.Mode getMode()
    {
        return this.mode;
    }
    
    /**
     * Sets the Mode, which governs the Tokenizer's behavior.<br>
     * 
     * Note: Most Token types are provided regardless - the mode is only used
     * in those cases where syntax is ambiguous without context.
     * 
     * @param mode
     */
    public void setMode(Tokenizer.Mode mode)
    {
        this.mode = mode;
    }
    
    
    
    /**
     * (Internally) Ascertains the next Token.
     * 
     * @return A Token, or null if reached end-of-life.
     * @throws LexicalError
     */
    private Token consumeNext() throws LexicalError
    {
        fromTop: while (true) {
            if (this.isEof()) {
                return null;
            }
            this.skipWhitespaceAndComments();
            
            int[] startPos;
            Token token;
            char c = this.safeCharAt();
            switch (c) {

                case '/':
                    // MULTI-LINE COMMENT
                    if (this.safeCharAt(this.currentCol + 1) == '*') {
                        startPos = this.getCurrentPos();
                        if (this.skipMultiLineComment()) {
                            // spans several lines - acts as STATEMENT SEPARATOR
                            return new Token(Token.Type.STATEMENT_SEPARATOR, this.getOrigin(startPos));
                        }
                        continue fromTop;
                    }
                    break;
            
                // DIRECTIVE
                case '.':
                    // may be a label instead
                    token = this.consumeLabel();
                    if (token != null) {
                        return token;
                    }
                    
                    token = this.consumeNamed(Token.Type.DIRECTIVE);
                    if (token != null) {
                        return token;
                    }
                    
                    // ..
                    if (
                        this.mode == Tokenizer.Mode.LENGTH
                        && this.safeCharAt(this.currentCol + 1) == '.'
                    ) {
                        return this.fromSymbol(Token.Type.LENGTH_RANGE, 2);
                    }
                    
                    return this.expectLabelName();
                
                // FUNCTION NAME
                case '&':
                    return this.expectNamed(Token.Type.FUNCTION_NAME);
                
                // STRING
                case '"':
                    return this.expectString();
                
                case '\'':
                    if (this.safeCharAt(this.currentCol + 1) == '\'') {
                        
                        // BIT LENGTH
                        return this.fromSymbol(Token.Type.BIT_LENGTH, 2);
                    }
                    // BIT POSITION
                    return this.fromSymbol(Token.Type.BIT_POSITION, 1);
                    
                case ':':
                    return this.fromSymbol(Token.Type.POSITION_RANGE, 1);
                
                // STATEMENT SEPARATOR
                case '\n':
                    token = new Token(Token.Type.STATEMENT_SEPARATOR, this.getOrigin());
                    this.advanceLine();
                    return token;
                case ';':
                    return this.fromSymbol(Token.Type.STATEMENT_SEPARATOR, 1);
            }
            
            if (this.mode == Tokenizer.Mode.META || this.mode == Tokenizer.Mode.LENGTH) {
                switch (c) {
                    // DATATYPE
                    case '/':
                        return this.expectNamed(Token.Type.DATATYPE);
                        
                    case '}':
                        return this.fromSymbol(Token.Type.CLOSING_BRACES, 1);
                    case '{':
                        return this.fromSymbol(Token.Type.OPENING_BRACES, 1);
                }
            }
            
            // EXPRESSION types
            if (this.mode == Tokenizer.Mode.LENGTH) {
                switch (c) {
                    case '>':
                        if (this.safeCharAt(this.currentCol + 1) == '=') {
                            // >=
                            return this.fromSymbol(Token.Type.LENGTH_GREATER_THAN_OR_EQUALS, 2);
                        }
                    case '<':
                        if (this.safeCharAt(this.currentCol + 1) == '=') {
                            // <=
                            return this.fromSymbol(Token.Type.LENGTH_LESS_THAN_OR_EQUALS, 2);
                        }
                    
                     // max / maxu
                    case 'm':
                    case 'M':
                        String word = this.restOfLine().substring(0, 4).toLowerCase();
                        if (word.equals("maxu")) {
                            return this.fromSymbol(Token.Type.LENGTH_MAXU, 4);
                        } else if (word.substring(0, 3).equals("max")) {
                            return this.fromSymbol(Token.Type.LENGTH_MAX, 3);
                        }
                }
            }
            
            token = this.consumeCommandSymbols();
            if (token != null) {
                return token;
            }
            
            // LABEL
            if (this.mode != Tokenizer.Mode.POSITION) {
                token = this.consumeLabel();
                if (token != null) {
                    return token;
                }
            }
            
            // NAME
            startPos = this.getCurrentPos();
            String name = this.consumeName();
            if (name != null) {
                return new Token(Token.Type.NAME, name, this.getOrigin(startPos));
            }
            
            // LABEL NAME or NUMBER
            token = this.consumeLabelNameOrNumber();
            if (token != null) {
                return token;
            }
            
            throw new LexicalError("Syntax error at " + this.getOrigin());
        }
    }
    
    
    /**
     * Creates a Token without content, and consumes a given amount of chars.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @param type   of the new Token
     * @param length How many chars to consume
     * 
     * @return
     */
    private Token fromSymbol(Token.Type type, int length)
    {
        int[] startPos = this.getCurrentPos();
        this.currentCol += length;
        
        return new Token(type, this.getOrigin(startPos));
    }
    
    /**
     * Creates a Token with content, and consumes a corresponding amount of
     * chars.<br>
     * 
     * This can be used when the Token contains the entire given content, and
     * this.currentCol is still pointing to the beginning of content (e.g. after
     * a regex match).<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @param type
     * @param content
     * 
     * @return
     */
    private Token fromContent(Token.Type type, String content)
    {
        int[] startPos = this.getCurrentPos();
        this.currentCol += content.length();
        
        return new Token(type, content, this.getOrigin(startPos));
    }
    
    
    /**
     * Consumes a section of command Symbols.
     * 
     * Does a tentative parse.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return Token if successfully consumed Command Symbols, null otherwise
     * @throws LexicalError If an escape char is encountered that is not
     *                      followed by a valid command symbol.
     */
    private Token consumeCommandSymbols() throws LexicalError
    {
        char c = this.safeCharAt();
        int[] startPos = this.getCurrentPos();
        
        if (c == '\\') {
            c = this.handleEscapedSymbol();
        } else if (!this.isCommandSymbol(c)) {
            return null;
        }
        
        StringBuilder text = new StringBuilder();
        do {
            text.append(c);
            this.currentCol++;
            this.skipWhitespaceAndComments();
            c = this.safeCharAt();
            if (c == '\\') {
                c = this.handleEscapedSymbol();
            }
        } while (this.isCommandSymbol(c));
        
        return new Token(Token.Type.COMMAND_SYMBOLS, text.toString(), this.getOrigin(startPos));
    }
    
    /**
     * @param c
     * @return True if given c is a command symbol.
     */
    private boolean isCommandSymbol(char c)
    {
        if (this.mode != Tokenizer.Mode.MAIN) {
            // These symbols do not need to be escaped in the head of a command
            // definition
            return Tokenizer.META_MODE_COMMAND_SYMBOLS.indexOf(c) != -1;
        }
        return Tokenizer.COMMAND_SYMBOLS.indexOf(c) != -1;
    }
    
    /**
     * Handles the \ escape char, which marks one char as a command symbol.<br>
     * 
     * Should only be used by {@link #consumeCommandSymbols()}. Is called when
     * the escape char has been detected.<br>
     * 
     * Before: Current position is ON the \ escape char. After: Position is ON
     * the next char.
     * 
     * @return The command symbol protected by the escape char.
     * @throws LexicalError If escape char is not followed by a valid command
     *                      symbol (i.e. the escape char cannot be used
     *                      everywhere).
     */
    private char handleEscapedSymbol() throws LexicalError
    {
        this.currentCol++;
        char c = this.safeCharAt();
        
        if (Tokenizer.COMMAND_SYMBOLS.indexOf(c) == -1) {
            throw new LexicalError("Expected escaped command symbol at " + this.getOrigin());
        }
        return c;
    }
    
    
    /**
     * Consumes a label. I.e. a label name directly followed by ':'.
     * 
     * Thus, the syntax is not ambiguous in this case, as the ':' char cannot be
     * used anywhere else.<br>
     * 
     * Does a tentative parse.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return Token if successfully consumed label, null otherwise
     */
    private Token consumeLabel()
    {
        Matcher m = Tokenizer.LABEL_PATTERN.matcher(this.restOfLine());
        if (!m.find()) {
            return null;
        }
        
        int[] startPos = this.getCurrentPos();
        this.currentCol += m.group().length();
        
        return new Token(Token.Type.LABEL, m.group(1), this.getOrigin(startPos));
    }
    
    /**
     * Consumes a label name.<br>
     * 
     * This should only be used when a label name is expected (i.e. all other
     * options have been exhausted at this point).<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return
     * @throws LexicalError If no valid label name found.
     */
    private Token expectLabelName() throws LexicalError
    {
        Matcher m = Tokenizer.LABEL_NAME_PATTERN.matcher(this.restOfLine());
        if (!m.find()) {
            throw new LexicalError("Expected label name at " + this.getOrigin());
        }
        
        return this.fromContent(Token.Type.LABEL, m.group());
    }
    
    /**
     * Consumes a label name or a number, whatever fits better.
     * 
     * Preferable returns a Number Token (which is syntactically a subset of a
     * label name).<br>
     * 
     * Does a tentative parse.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return Token if successfully consumed number, null otherwise
     * @throws LexicalError if leading '-' found but it's not part of a valid
     *                      negative number
     */
    private Token consumeLabelNameOrNumber() throws LexicalError
    {
        // negative NUMBER
        if (this.safeCharAt() == '-') {
            Matcher m = Tokenizer.NEGATIVE_NUMBER_PATTERN.matcher(this.restOfLine());
            if (!m.find()) {
                throw new LexicalError("Expected number at " + this.getOrigin());
            }
            return this.fromContent(Token.Type.NUMBER, m.group());
        }
        
        // LABEL NAME or NUMBER
        Matcher m = Tokenizer.LABEL_NAME_PATTERN.matcher(this.restOfLine());
        if (!m.find()) {
            return null;
        }
        String matched = m.group();
        
        // This works because every valid number (except negative) is also a valid label name
        m = Tokenizer.FULL_NUMBER_PATTERN.matcher(matched);
        if (!m.matches()) {
            return this.fromContent(Token.Type.LABEL, matched);
        }
        return this.fromContent(Token.Type.NUMBER, matched);
    }
    
    /**
     * Consumes a named entity, i.e. an entity that has a name which makes up
     * the content of the resulting Token.<br>
     * 
     * Starts ON the symbol that starts the entity; the name is expected to
     * start on the next char.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @see #consumeNamed()
     * @param type of the new Token
     * @return
     * @throws LexicalError If no name found
     */
    private Token expectNamed(Token.Type type) throws LexicalError
    {
        Token token = this.consumeNamed(type);
        if (token == null) {
            throw new LexicalError("Expected name at " + this.getOrigin());
        }
        return token;
    }
    
    /**
     * Tentatively consumes a named entity, i.e. an entity that has a name which
     * makes up the content of the resulting Token.<br>
     * 
     * Starts ON the symbol that starts the entity; the name is expected to
     * start on the next char.<br>
     * 
     * Opposed to {@link #expectNamed()}, this does a tentative parse, and
     * throws no Exception.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @param type
     * @return Token if successfully consumed label, null otherwise
     */
    private Token consumeNamed(Token.Type type)
    {
        int[] startPos = this.getCurrentPos();
        this.currentCol++;
        String name = this.consumeName();
        if (name == null) {
            return null;
        }
        
        return new Token(type, name, this.getOrigin(startPos));
    }
    
    
    /**
     * Consumes a name, which may be used as part of a Token.
     * 
     * Does a tentative parse.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return Name if successfully consumed, null otherwise
     */
    private String consumeName()
    {
        Matcher m = Tokenizer.NAME_PATTERN.matcher(this.restOfLine());
        if (!m.find()) {
            return null;
        }
        
        String name = m.group();
        this.currentCol += name.length();
        
        return name;
    }
    
    /**
     * Consumes a string entity.<br>
     * 
     * Processes the string so that it's actual content is used as the Token
     * content (i.e. encodings like "\n" are replaced by the actual chars they
     * are supposed to represent).<br>
     * 
     * Current position MUST point to opening ".<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return
     * @throws LexicalError If file ends before String is closed.
     */
    private Token expectString() throws LexicalError
    {
        int[] startPos = this.getCurrentPos();
        this.currentCol++;
        StringBuilder text = new StringBuilder();
        
        char c = this.safeCharAt();
        while (c != '"') {
            if (c == '\\') {
                this.currentCol++;
                c = this.safeCharAt();
                switch (c) {
                    case 'n':
                        text.append('\n');
                        break;
                    case 'r':
                        text.append('\r');
                        break;
                    case 't':
                        text.append('\t');
                        break;
                        // TODO any other codes?
                    default:
                        text.append(c);
                        break;
                }
            } else {
                text.append(c);
            }
            if (c == '\n') {
                if (!this.advanceLine()) {
                    throw new LexicalError(
                        "Unexpected end of file, unclosed string at " + new Origin(this.file, startPos)
                    );
                }
            }
            
            this.currentCol++;
            c = this.safeCharAt();
        }
        
        this.currentCol++;
        return new Token(Token.Type.STRING, text.toString(), this.getOrigin(startPos));
    }
    
    
    /**
     * Stops ON next non-whitespace and non-comment char.<br>
     * 
     * Does not handle multi-line comments.<br>
     * 
     * DO NOT invoke if at end-of-file.
     */
    private void skipWhitespaceAndComments()
    {
        while (this.currentCol < this.lineContent.length()) {
            
            switch (this.charAt()) {
                case ' ': // Whitespace
                case '\t':
                    this.currentCol++;
                    break;
                    
                case '/': // Comment?
                    if (this.safeCharAt(this.currentCol + 1) != '/') {
                        return;
                    }
                    // Fall-through
                    
                case '#': // Single-line comment
                    this.currentCol = this.lineContent.length(); // Put at end of line
                    return;
                    
                default: // Nothing else to skip
                    return;
            }
        }
    }
    
    /**
     * Skips a multi-line comment.
     * 
     * Current position MUST point to the first char of the '/*' symbol.
     * 
     * @return True if comment spans more than one line.
     * @throws LexicalError If file ends before comment is closed.
     */
    private boolean skipMultiLineComment() throws LexicalError
    {
        int[] startPos = this.getCurrentPos();
        this.currentCol += 2; // Skip '/*'
        
        boolean multiLine = false;
        while (true) {
            while (this.currentCol >= this.lineContent.length()) {
                multiLine = true;
                if (!this.advanceLine()) {
                    throw new LexicalError(
                        "Unexpected end of file, unclosed multi-line comment at " + new Origin(this.file, startPos)
                    );
                }
            }
            
            switch (this.charAt()) {
                case '*': // End of comment?
                    if (this.safeCharAt(this.currentCol + 1) == '/') {
                        this.currentCol += 2;
                        return multiLine;
                    }
                    // Fall-through
                    
                default:
                    this.currentCol++;
            }
        }
    }
    
    /**
     * Advances internal state to the next line.
     * 
     * @return True if successful, false if we reached end-of-file
     */
    private boolean advanceLine()
    {
        this.currentLine++;
        this.currentCol = 0;
        if (this.file.lines.size() > this.currentLine) {
            this.lineContent = file.lines.get(this.currentLine);
            return true;
        }
        
        this.lineContent = null;
        return false;
    }
    
    /**
     * Returns the char at the given position in this.lineContent. Different
     * to {@link String#charAt()}, this does not throw an exception.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @param col
     * @return The char at col position in this.lineContent, or '\n' if this
     *         position is too big for this string.
     */
    private char safeCharAt(int col)
    {
        if (col >= this.lineContent.length()) {
            return '\n';
        }
        return this.lineContent.charAt(col);
    }
    
    /**
     * Shorthand for {@code this.lineContent.safeCharAt(this.currentCol)}.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return The char at this.currentCol position in this.lineContent, or '\n'
     *         if this position is too big for this string.
     */
    private char safeCharAt()
    {
        return this.safeCharAt(this.currentCol);
    }
    
    /**
     * Shorthand for {@code this.lineContent.charAt(this.currentCol)}.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return
     */
    private char charAt()
    {
        return this.lineContent.charAt(this.currentCol);
    }
    
    /**
     * Returns the rest of the current line, i.e. from the current position
     * (inclusive) until end-of-line (exclusive).<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return
     */
    private String restOfLine()
    {
        if (this.currentCol > this.lineContent.length()) {
            return "";
        }
        return this.lineContent.substring(this.currentCol);
    }
    
    
    /**
     * @return True if end-of-file has been reached
     */
    private boolean isEof()
    {
        return this.lineContent == null;
    }
    
    /**
     * Returns the current line and col position.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @param minus1 True: subtracts 1 from col, i.e. gets the position for
     *               previous char. DO NOT use true directly after
     *               {@link #advanceLine()}.
     * @return [line, col]
     */
    private int[] getCurrentPos(boolean minus1)
    {
        return new int[]{this.currentLine + 1, this.currentCol + (minus1 ? 0 : 1)};
    }
    
    /**
     * Returns the current line and col position.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return [line, col]
     */
    private int[] getCurrentPos() {
        return this.getCurrentPos(false);
    }
    
    /**
     * Creates a new Origin pointing to an area.
     * 
     * The area starts at startPos and ends at the current position.<br>
     * 
     * DO NOT invoke if at end-of-file.<br>
     * 
     * This always uses minus1 for the end position (see
     * {@link #getCurrentPos(boolean) getCurrentPos()}).
     * 
     * @param startPos
     * @return
     */
    private Origin getOrigin(int[] startPos)
    {
        return new Origin(this.file, startPos, this.getCurrentPos(true));
    }
    
    /**
     * Creates a new Origin pointing to a position.
     * 
     * Uses the current position.<br>
     * 
     * DO NOT invoke if at end-of-file.
     * 
     * @return
     */
    private Origin getOrigin()
    {
        return new Origin(this.file, this.getCurrentPos(false));
    }
}
