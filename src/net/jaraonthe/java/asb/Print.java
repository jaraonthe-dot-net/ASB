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
     * @param settings May be null
     */
    public static void printWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.print(Print.getEffectiveWithColor(text, color, false, settings));
    }
    
    /**
     * Does a print() with given color and in bold. Honors {@code
     * settings.withColor}.
     * 
     * @param text
     * @param color
     * @param settings May be null
     */
    public static void printBoldWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.print(Print.getEffectiveWithColor(text, color, true, settings));
    }
    
    /**
     * Does a println() with given color. Honors {@code settings.withColor}.
     * 
     * @param text
     * @param color
     * @param settings May be null
     */
    public static void printlnWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.println(Print.getEffectiveWithColor(text, color, false, settings));
    }
    
    /**
     * Does a println() with given color and in bold. Honors {@code
     * settings.withColor}.
     * 
     * @param text
     * @param color
     * @param settings May be null
     */
    public static void printlnBoldWithColor(String text, Print.Color color, Settings settings)
    {
        System.out.println(Print.getEffectiveWithColor(text, color, true, settings));
    }
    
    
    /**
     * @param text
     * @param color
     * @param bold
     * @param settings May be null
     * 
     * @return The given text with color applied
     */
    private static String getEffectiveWithColor(String text, Print.Color color, boolean bold, Settings settings)
    {
        if (settings == null || !settings.withColor()) {
            return text;
        }
        return "\033[" + (bold ? "1;" : "") + color.foregroundCode + "m" + text + "\033[0m";
    }
    
    
    private Print()
    {
        // Nothing
    }
}
