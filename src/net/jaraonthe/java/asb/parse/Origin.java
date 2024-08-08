package net.jaraonthe.java.asb.parse;

/**
 * An entity's (e.g. Token, AST entity) location within an ASB Source code file.<br>
 * 
 * Positions are given in human-readable format, i.e. starting at 1 (for both
 * line and col data).
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Origin
{
	/**
	 * The file where this entity originates.
	 */
	public final SourceFile file;
	
	/**
	 * The line in the file where the entity starts. First line in the file is
	 * number 1.
	 */
	public final int startLine;
	
	/**
	 * The position in the start line where the entity starts. Beginning of a
	 * line is position 1.
	 */
	public final int startCol;
	
	/**
	 * The line in the file where the entity ends. First line in the file is
	 * number 1.
	 */
	public final int endLine;

	/**
	 * The position in the end line where the entity ends. Beginning of a
	 * line is position 1.<br>
	 * 
	 * This points to the position ON WHICH the last character of the token
	 * resides. We assume that tokens do not have a length of 0.
	 */
	public final int endCol;

	/**
	 * Constructs a new Origin pointing to an area in the given file, defined
	 * with start and end positions.<br>
	 * 
	 * Positions are 1-based, i.e. both lines and cols start at 1.<br>
	 * 
	 * Arguments must point to valid positions within file (see
	 * {@link SourceFile#isValidPosition(int, int)}).
	 * 
	 * @param file
	 * @param startLine
	 * @param startCol
	 * @param endLine   >= startLine
	 * @param endCol    if (endLine == startLine) endCol >= startCol
	 */
	public Origin(
	    SourceFile file,
	    int startLine,
	    int startCol,
	    int endLine,
	    int endCol
    ) {
	    // I would love to use asserts, but as they can be switched off, they
	    // are completely useless - boilerplate here we go!
	    if (endLine < startLine) {
	        throw new IllegalArgumentException("endLine cannot be smaller than startLine");
	    }
        if (endLine == startLine && endCol < startCol) {
            throw new IllegalArgumentException("endCol cannot be smaller than startCol (on single-line entity)");
        }
        if (!file.isValidPosition(startLine, startCol)) {
            throw new IllegalArgumentException("Invalid start position");
        }
        if (!file.isValidPosition(endLine, endCol)) {
            throw new IllegalArgumentException("Invalid end position");
        }
	    
		this.file      = file;
		this.startLine = startLine;
		this.startCol  = startCol;
		this.endLine   = endLine;
		this.endCol    = endCol;
	}
	
	/**
	 * Constructs a new Origin pointing to an area in the given file, defined
     * with start and end positions.<br>
     * 
     * Positions are 1-based, i.e. both lines and cols start at 1.<br>
     * 
     * Arguments must point to valid positions within file (see
     * {@link SourceFile#isValidPosition(int, int)}).
	 * 
	 * @param file
	 * @param startPos [startLine, startCol]
	 * @param endPos   [endLine, endCol]. MUST NOT point to a position before
	 *                 startPos.
	 */
	public Origin(SourceFile file, int[] startPos, int[] endPos)
	{
	    this(file, startPos[0], startPos[1], endPos[0], endPos[1]);
	}

	/**
	 * Constructs a new Origin pointing to a position in the given file.<br>
	 * 
	 * This is equivalent to pointing to an area that is exactly one char long.<br>
     * 
     * Position is 1-based, i.e. both line and col start at 1.<br>
     * 
     * Arguments must point to a valid position within file (see
     * {@link SourceFile#isValidPosition(int, int)}).
	 * 
	 * @param file
	 * @param line
	 * @param col
	 */
	public Origin(SourceFile file, int line, int col)
	{
		this(file, line, col, line, col);
	}
    
    /**
     * Constructs a new Origin pointing to a position in the given file.<br>
     * 
     * This is equivalent to pointing to an area that is exactly one char long.<br>
     * 
     * Position is 1-based, i.e. both line and col start at 1.<br>
     * 
     * Argument must point to a valid position within file (see
     * {@link SourceFile#isValidPosition(int, int)}).
     * 
	 * @param file
	 * @param pos  [line, col]
	 */
    public Origin(SourceFile file, int[] pos)
    {
        this(file, pos[0], pos[1], pos[0], pos[1]);
    }
	
	
	/**
	 * @return True if this entity spans more than one line, or in other words:
	 *         this entity ends on a different line than where it starts.
	 */
	public boolean isMultiLine()
	{
	    return this.startLine != this.endLine;
	}
	
	/**
	 * @return The file content this entity points to. If a multiLine entity,
	 *         lines are separated with \n.
	 */
	public String getContent()
	{
	    if (this.startLine == this.endLine) {
	        String line = this.file.lines.get(this.startLine - 1);
	        String text = line.substring(
	            this.startCol - 1,
	            Math.min(this.endCol, line.length()) // in case we point to eol char
            );
	        if (this.endCol > line.length()) { // in case we point to eol char
	            text += '\n';
	        }
	        return text;
	    }
	    
	    // Multi-line
	    StringBuilder text = new StringBuilder(
	        this.file.lines.get(this.startLine - 1).substring(this.startCol - 1)
        );
	    for (int i = this.startLine; i < this.endLine - 1; i++) { // i is 0-based
	        text.append('\n');
	        text.append(this.file.lines.get(i));
	    }
	    text.append('\n');
	    String endLine = this.file.lines.get(this.endLine - 1);
	    text.append(endLine.substring(
            0,
            Math.min(this.endCol, endLine.length()) // in case we point to eol char
        ));
	    
	    return text.toString();
	}
    
    /**
     * @return The file content of the lines that this entity points to
     *         (returning entire lines as opposed to {@link #getContent()}). If
     *         a multiLine entity, lines are separated with \n.
     */
	// TODO Remove if not used anywhere
    public String getLineContent()
    {
        StringBuilder text = new StringBuilder(this.file.lines.get(this.startLine - 1));
        // TODO In case endCol points to the end-of-line, the newline char
        //      is NOT included in the result (as opposed to getContent())
        
        for (int i = this.startLine; i < this.endLine; i++) { // i is 0-based
            text.append('\n');
            text.append(this.file.lines.get(i));
        }
        return text.toString();
    }
    
    @Override
    public String toString()
    {
        StringBuilder text = new StringBuilder(this.file.filePath.toString());
        text.append(':');
        text.append(this.startLine);
        text.append(':');
        text.append(this.startCol);
        
        if (this.endLine != this.startLine) {
            text.append('-');
            text.append(this.endLine);
            text.append(':');
            text.append(this.endCol);
        } else if (this.endCol != this.startCol) {
            // ends on start line
            text.append('-');
            text.append(this.endCol);
        }
        
        return text.toString();
    }
}
