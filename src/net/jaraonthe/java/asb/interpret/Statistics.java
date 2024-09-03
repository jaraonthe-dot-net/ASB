package net.jaraonthe.java.asb.interpret;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jaraonthe.java.asb.ast.invocation.CommandInvocation;
import net.jaraonthe.java.asb.ast.invocation.Invocation;

/**
 * Gathers statistics during the interpretation phase.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Statistics
{
    /**
     * Tracks how often each command has been invoked (in userland code).<br>
     * 
     * command identity => count for this command
     */
    private final Map<String, Integer> invocationsCount = new HashMap<>();
    
    /**
     * Increments the command invocation counter. This should be called every
     * time a userland invocation is executed.
     * 
     * @param invocation
     */
    public void incrementInvocationsCount(Invocation invocation)
    {
        if (!(invocation instanceof CommandInvocation)) {
            return;
        }
        CommandInvocation ci = (CommandInvocation) invocation;
        String commandIdentity = ci.getInvokedCommand().getIdentity();
        
        this.invocationsCount.put(
            commandIdentity,
            this.invocationsCount.getOrDefault(commandIdentity, 0) + 1
        );
    }
    
    /**
     * 
     * 
     * @return How often each command has been invoked (in userland code).<br>
     *         command identity => count for this command
     */
    public Map<String, Integer> getInvocationsCount()
    {
        return Collections.unmodifiableMap(this.invocationsCount);
    }
}
