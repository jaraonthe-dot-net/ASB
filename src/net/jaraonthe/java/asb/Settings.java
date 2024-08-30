package net.jaraonthe.java.asb;

import java.util.ArrayList;
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
    public final List<String> filePaths = new ArrayList<>();
    
    /**
     * True: dev mode enabled, which changes application behavior to be more
     * verbose. This is an inofficial feature.
     */
    public boolean devMode = false;
    
    /**
     * True: Use color in output.
     */
    public boolean withColor = true;
    
    
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
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].isBlank()) {
                // Just to be safe
                continue;
            }
            if (args[i].charAt(0) != '-') {
                regularFilePaths.add(args[i]);
                continue;
            }
            
            switch (args[i]) {
                case "--dev-mode":
                    settings.devMode = true;
                    break;
                    
                case "-C":
                case "--no-color":
                    settings.withColor = false;
                    break;
                    
                case "-h":
                case "--help":
                    settings.setMode(Settings.Mode.HELP, args[i]);
                    break;
                    
                case "-v":
                case "--version":
                    settings.setMode(Settings.Mode.VERSION, args[i]);
                    break;
                    
                case "--about":
                    settings.setMode(Settings.Mode.ABOUT, args[i]);
                    break;
                    
                case "-i":
                case "--include":
                    i++;
                    if (args[i].charAt(0) == '-') {
                        throw new UserError(
                            "Expected file after " + args[i - 1] + " argument. See asb --help"
                        );
                    }
                    settings.filePaths.add(args[i]);
                    break;
                    
                default:
                    throw new UserError(
                        "Unknown CLI argument \"" + args[i] + "\". See asb --help"
                    );
            }
        }
        
        settings.filePaths.addAll(regularFilePaths);
        if (settings.mode == null) {
            settings.setMode(Settings.Mode.MAIN, "");
        }
        
        return settings;
    }
}
