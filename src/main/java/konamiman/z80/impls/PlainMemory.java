package konamiman.z80.impls;

import java.util.Arrays;

import konamiman.z80.interfaces.Memory;


/**
 * Represents a trivial memory implementation in which all the addresses are RAM
 * and the values written are simply read back. This is the default implementation
 * of {@link Memory}.
 */
public class PlainMemory implements Memory {

    private final byte[] memory;

    public PlainMemory(int size) {
        if (size < 1)
            throw new IllegalArgumentException("Memory size must be greater than zero");

        memory = new byte[size];
        this.size = size;
    }

    private int size;

    public int getSize() {
        return size;
    }

    /**
     * @param address should be positive value
     */
    public byte get(int address) {
        return memory[address];
    }

    /**
     * @param address should be positive value
     */
    public void set(int address, byte value) {
        memory[address] = value;
    }

    public void setContents(int startAddress, byte[] contents, int startIndex/*= 0*/, Integer length/*= null*/) {
        if (contents == null)
            throw new NullPointerException("contents");

        if (length == null)
            length = contents.length;

        if ((startIndex + length) > contents.length)
            throw new IndexOutOfBoundsException("startIndex + length cannot be greater than contents.length");

        if (startIndex < 0)
            throw new IndexOutOfBoundsException("startIndex cannot be negative");

        if (startAddress + length > size)
            throw new IndexOutOfBoundsException("startAddress + length cannot go beyond the memory size");

        System.arraycopy(contents, startIndex, memory, startAddress, length);
    }

    public byte[] getContents(int startAddress, int length) {
        if (startAddress >= memory.length)
            throw new IndexOutOfBoundsException("startAddress cannot go beyond memory size");

        if (startAddress + length > memory.length)
            throw new IndexOutOfBoundsException("startAddress + length cannot go beyond memory size");

        if (startAddress < 0)
            throw new IndexOutOfBoundsException("startAddress cannot be negative");

        return Arrays.copyOfRange(memory, startAddress, startAddress + length);
    }
}
