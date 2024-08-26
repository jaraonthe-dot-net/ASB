package net.jaraonthe.java.asb.interpret;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import net.jaraonthe.java.asb.exception.RuntimeError;
import net.jaraonthe.java.asb.interpret.value.NumericValueStore;

/**
 * The data memory of the virtual system.
 *
 * @author Jakob Rathbauer <jakob@jaraonthe.net>
 */
public class Memory
{
    /**
     * The length of one memory word.
     */
    public final int wordLength;
    
    /**
     * The length of a memory address.
     */
    public final int addressLength;
    
    /**
     * memory address => word
     */
    private Map<BigInteger, BigInteger> words;
    
    private static final int INITIAL_CAPACITY_BITS = 8192; // 1KiB
    
    /**
     * @param wordLength    The length of one memory word. Must be a valid length.
     * @param addressLength The length of a memory address. Must be a valid length.
     */
    public Memory(int wordLength, int addressLength)
    {
        this.wordLength    = wordLength;
        this.addressLength = addressLength;
        
        // Reserving ~1kiB worth of virtual system memory to start with...
        int initialCapacity = Math.ceilDiv(Memory.INITIAL_CAPACITY_BITS, wordLength);
        if (Math.log(initialCapacity) / Math.log(2) > addressLength) {
            // ... but don't reserve more than can be addressed
            initialCapacity = (int)Math.pow(2, addressLength);
        }
        // Of course, the actual memory cells are lazily instantiated as we go along
        
        this.words = HashMap.newHashMap(initialCapacity);
    }
    
    /**
     * Reads the memory word at given address.
     * 
     * @param address
     * @return
     * @throws RuntimeError
     */
    public BigInteger read(BigInteger address) throws RuntimeError
    {
        address = this.checkAddress(address);
        
        BigInteger word = this.words.get(address);
        if (word == null) {
            return BigInteger.ZERO;
        }
        return word;
    }
    
    /**
     * Overwrites the memory word at the given address.
     * 
     * @param address
     * @param word
     */
    public void write(BigInteger address, BigInteger word)
    {
        address = this.checkAddress(address);
        
        if (word.signum() < 0) {
            word = NumericValueStore.normalizeBigInteger(word, this.wordLength);
        }
        if (NumericValueStore.bitLength(word) > this.wordLength) {
            throw new IllegalArgumentException("Value is too big for memory: " + word);
        }
        
        this.words.put(address, word);
    }
    
    /**
     * Checks that given address is valid, throws error otherwise
     * 
     * @param address
     * @throws IllegalArgumentException
     */
    private BigInteger checkAddress(BigInteger address)
    {
        if (address.signum() < 0) {
            address = NumericValueStore.normalizeBigInteger(address, this.addressLength);
        }
        if (address.bitLength() > this.addressLength) {
            throw new IllegalArgumentException("Memory address is too big: 0x" + address.toString(16));
        }
        return address;
    }
}
