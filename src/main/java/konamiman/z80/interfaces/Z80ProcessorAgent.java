package konamiman.z80.interfaces;


import konamiman.z80.Z80Processor;


/**
 * Represents a class that allows to perform a limited set of operations on an {@link Z80Processor}.
 */
public interface Z80ProcessorAgent extends ExecutionStopper {

    /**
     * Reads the next opcode byte from the memory location currently pointed by the PC register,
     * then increments PC by one.
     *
     * @return The byte obtained from memory
     */
    byte fetchNextOpcode();

    /**
     * Reads the next opcode byte from the memory location currently pointed by the PC register,
     * but does not modify PC.
     *
     * @return The byte obtained from memory
     *
     * <remarks> This method can be useful to handle 0xDD and 0xFD prefixes.
     * If {@link Z80InstructionExecutor#execute(byte)} receives one of these bytes, it can use this method
     * the check the next opcode byte. If both bytes do not form a supported instruction
     * (for example, if the second byte is another 0xDD/0xFD byte), then {@link Z80InstructionExecutor#execute(byte)}
     * simply returns; the first 0xDD/0xFD acts then as a NOP, and the
     * second byte will be fetched again for the next invocation of {@link Z80InstructionExecutor#execute(byte)}.
     * Otherwise, {@link #fetchNextOpcode()} in invoked in order to get PC properly incremented.
     */
    byte peekNextOpcode();

    /**
     * Reads one byte from memory.
     *
     * @param address a Memory address to read from
     * @return Obtained byte
     */
    byte readFromMemory(short address);

    /**
     * Writes one byte to memory.
     *
     * @param address a Memory address to write to
     * @param value Value to write
     */
    void writeToMemory(short address, byte value);

    /**
     * Reads one byte from an I/O port.
     *
     * @param portNumber Port number to read from
     * @return Obtained byte
     */
    byte readFromPort(byte portNumber);

    /**
     * Writes one byte to an I/O port.
     *
     * @param portNumber Port number to write to
     * @param value Value to write
     */
    void writeToPort(byte portNumber, byte value);

    /**
     * Returns the current register set used by the processor.
     */
    Z80Registers getRegisters();

    /**
     * Changes the current interrupt mode.
     *
     * @param interruptMode The new interrupt mode.
     * @throws IllegalArgumentException Attempt to set a value other than 0, 1 or 2
     */
    void setInterruptMode2(byte interruptMode);
}
