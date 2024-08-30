package net.jaraonthe.java.asb;

/**
 * This facilitates more advanced output to console, e.g. colors.
 *
 * @see https://en.wikipedia.org/wiki/ANSI_escape_code
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public final class Print
{
    /**
     * Color choices for output.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum Color
    {
        RED     (31),
        GREEN   (32),
        YELLOW  (33),
        BLUE    (34),
        MAGENTA (35),
        CYAN    (36),
        WHITE   (37);
        
        public final int foregroundCode;
        
        private Color(int foregroundCode) {
            this.foregroundCode = foregroundCode;
        }
    }
    
    
    /**
     * Does a print() with given color. Honors {@code settings.withColor}.
     * 
     * @param text
     * @param color
     * @param settings
     */
    public static void printWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.print(Print.getEffectiveWithColor(text, color, settings));
    }
    
    /**
     * Does a println() with given color. Honors {@code settings.withColor}.
     * 
     * @param text
     * @param color
     * @param settings
     */
    public static void printlnWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.println(Print.getEffectiveWithColor(text, color, settings));
    }
    
    /**
     * @param text
     * @param color
     * @param settings
     * 
     * @return The given text with color applied
     */
    private static String getEffectiveWithColor(String text, Print.Color color, Settings settings)
    {
        if (!settings.getWithColor()) {
            return text;
        }
        return "\033[" + color.foregroundCode + "m" + text + "\033[0m";
    }
    
    
    private Print()
    {
        // Nothing
    }
}
