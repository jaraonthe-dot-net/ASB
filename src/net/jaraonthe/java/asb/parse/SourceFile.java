package net.jaraonthe.java.asb.parse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Represents one ASB Source Code file.<br>
 * 
 * Contains the entire file contents for convenient use.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class SourceFile
{
    /**
     * Path to the source code file
     */
    public final Path filePath;
    
    /**
     * A list of all text lines in the file. This list cannot be modified.
     */
    public final List<String> lines;
    
    /**
     * @param filePath Absolute or relative path to file
     * @throws IOException
     */
    public SourceFile(String filePath) throws IOException
    {
        this(Paths.get(filePath));
    }
    
    /**
     * @param filePath
     * @throws IOException
     */
    public SourceFile(Path filePath) throws IOException
    {
        this.filePath = filePath.toAbsolutePath();
        this.lines    = Collections.unmodifiableList(Files.readAllLines(this.filePath));
    }
    
    
    /**
     * Return true if the given (1-based human-readable) position exists within
     * this file.<br>
     * 
     * Note: Last position on a line is the position of the end-of-line or
     * newline character, which always exists (even on the last line) and is
     * always one char long (even when \r\n is used as line separator in the
     * actual file). I.e. in empty lines col = 1 is a valid position.
     * 
     * @param line Line of position. First line in a file is 1.
     * @param col  Position within line. First position in a line is 1.
     * 
     * @return True if position exists
     */
    public boolean isValidPosition(int line, int col)
    {
        // Transforming between 1-based human-readable input & 0-based internal
        // representation
        if (this.lines.size() < line) {
            return false;
        }
        // Allowing col to point to additional end-of-line char which is omitted
        // in the internal representation
        return (this.lines.get(line - 1).length() + 1) >= col;
    }
    
    @Override
    public String toString()
    {
        return "SourceFile " + this.filePath;
    }
}
