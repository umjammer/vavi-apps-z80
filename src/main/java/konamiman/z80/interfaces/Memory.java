package konamiman.z80.interfaces;


/**
 * Represents the memory that is visible by a processor.
 */
public interface Memory {
    /**
     * Returns the size of the memory.
     */
    int getSize();

    /**
     * Reads or writes one single memory address.
     *
     * @param address Address to read or write
     * @return Data to write
     *
     * @throws IndexOutOfBoundsException <c>address</c> is negative or beyond the memory size.
     */
    byte get(int address);

    void set(int address, byte value);

    /**
     * Sets a portion of the memory with the contents of a byte array.
     *
     * @param startAddress First memory address that will be set
     * @param contents New contents of the memory
     * @param startIndex Start index for starting copying within the contents array
     * @param length Length of the contents array that will be copied. If null,
     * the whole array is copied.
     * @throws IndexOutOfBoundsException <c>startAddress</c> + <c>length</c> (or <c>content.Length</c>)
     * goes beyond the memory size, or <c>length</c> is greater that the actual length of <c>contents</c>.
     * @throws NullPointerException contents is null
     */
    void setContents(int startAddress, byte[] contents, int startIndex /* = 0 */, Integer length /* = null */);

    /**
     * Returns the contents of a portion of the memory as a byte array.
     *
     * @param startAddress First memory address whose contents will be returned
     * @param length Length of the portion to return
     * @return Current contents of the specified memory portion
     *
     * @throws UnsupportedOperationException <c>startAddress</c> + <c>length</c> (or <c>content.Length</c>)
     * goes beyond the memory size.
     */
    byte[] getContents(int startAddress, int length);
}
