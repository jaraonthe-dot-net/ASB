package net.jaraonthe.java.asb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jaraonthe.java.asb.exception.UserError;

/**
 * General application settings.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Settings
{
    /**
     * Application mode. This represents what is being done.
     *
     * @author Jakob Rathbauer <jakob@jaraonthe.net>
     */
    public enum Mode
    {
        MAIN,
        HELP,
        VERSION,
        ABOUT;
    }
    
    /**
     * The application mode. This selects what is being done.
     */
    private Settings.Mode mode;
    private String modeArg;
    
    /**
     * The files to be parsed, in parsing order.
     */
    private final List<String> filePaths = new ArrayList<>();
    
    /**
     * True: dev mode enabled, which changes application behavior to be more
     * verbose. This is an inofficial feature.
     */
    private boolean devMode = false;
    
    /**
     * True: Log every invocation.
     */
    private boolean trace = false;
    
    /**
     * True: Print command invocation statistics after interpretation.
     */
    private boolean statistics = false;
    
    /**
     * True: Print register values after interpretation.
     */
    private boolean registers = false;
    
    /**
     * True: Use color in output.
     */
    private boolean withColor = true;
    
    /**
     * Transitive state.
     * 
     * True: A &print or &print_* action has occurred. Shall be reset to false
     * once a newline char is printed.
     */
    public boolean printOccurred = false;
    
    
    private Settings()
    {
        // nothing
    }
    
    /**
     * Sets the application mode.
     * 
     * @param mode
     * @param arg  The argument string that was used to select the mode
     * 
     * @throws UserError if mode has already been set
     */
    private void setMode(Settings.Mode mode, String arg) throws UserError
    {
        if (this.mode != null) {
            throw new UserError(
                "Cannot use " + arg + " as " + this.modeArg + " has already been used"
            );
        }
        this.mode = mode;
        this.modeArg = arg;
    }
    
    /**
     * @return The application mode
     */
    public Settings.Mode getMode()
    {
        return this.mode;
    }
    
    /**
     * @return The files to be parsed, in parsing order
     */
    public List<String> getFilePaths()
    {
        return Collections.unmodifiableList(this.filePaths);
    }
    
    /**
     * @return True: dev mode enabled, which changes application behavior to be
     *         more verbose. This is an inofficial feature.
     */
    public boolean devMode()
    {
        return this.devMode;
    }
    
    /**
     * @return True: Log every invocation
     */
    public boolean trace()
    {
        return this.trace;
    }
    
    /**
     * @return True: Print command invocation statistics after interpretation
     */
    public boolean statistics()
    {
        return this.statistics;
    }
    
    /**
     * @return True: Print register values after interpretation
     */
    public boolean registers()
    {
        return this.registers;
    }
    
    /**
     * @return True: Use color in output
     */
    public boolean withColor()
    {
        return this.withColor;
    }
    
    
    /**
     * Parses CLI args into application settings.
     * 
     * @param args
     * @return
     * @throws UserError
     */
    static Settings fromArgs(String[] args) throws UserError
    {
        Settings settings = new Settings();
        List<String> regularFilePaths = new ArrayList<>();
        
        boolean expectFile = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].isBlank()) {
                // Just to be safe
                continue;
            }
            if (expectFile) {
                // After --include flag
                settings.filePaths.add(args[i]);
                expectFile = false;
                continue;
            }
            
            if (args[i].charAt(0) != '-') {
                regularFilePaths.add(args[i]);
                continue;
            }
            
            String[] argParts;
            if (args[i].length() >= 2 && args[i].charAt(1) != '-') {
                // short options - take the arg apart into individual short options
                argParts = new String[args[i].length() - 1];
                for (int j = 1; j < args[i].length(); j++) {
                    argParts[j - 1] = "-" + args[i].charAt(j);
                }
            } else {
                argParts = new String[]{args[i]};
            }
            
            for (String argPart : argParts) {
                switch (argPart) {
                    case "--dev-mode":
                        settings.devMode = true;
                        break;
                        
                    case "-t":
                    case "--trace":
                        settings.trace = true;
                        break;
                        
                    case "-s":
                    case "--statistics":
                        settings.statistics = true;
                        break;
                        
                    case "-r":
                    case "--registers":
                        settings.registers = true;
                        break;
                        
                    case "-C":
                    case "--no-color":
                        settings.withColor = false;
                        break;
                        
                    case "-h":
                    case "--help":
                        settings.setMode(Settings.Mode.HELP, argPart);
                        break;
                        
                    case "-v":
                    case "--version":
                        settings.setMode(Settings.Mode.VERSION, argPart);
                        break;
                        
                    case "--about":
                        settings.setMode(Settings.Mode.ABOUT, argPart);
                        break;
                        
                    case "-i":
                    case "--include":
                        expectFile = true;
                        break;
                        
                    default:
                        throw new UserError(
                            "Unknown CLI argument \"" + argPart + "\". See asb --help"
                        );
                }
            }
        }
        if (expectFile) {
            throw new UserError(
                "Expected file after " + args[args.length - 1] + " argument. See asb --help"
            );
        }
        
        settings.filePaths.addAll(regularFilePaths);
        if (settings.mode == null) {
            settings.setMode(Settings.Mode.MAIN, "");
        }
        
        return settings;
    }
}
