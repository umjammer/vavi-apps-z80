package konamiman.DependenciesInterfaces;


/**
 * Represents a class that allows to perform a limited set of operations on an {@link IZ80Processor}.
 */
public interface IZ80ProcessorAgent extends IExecutionStopper {

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
     * If {@link IZ80InstructionExecutor.Execute} receives one of these bytes, it can use this method
     * the check the next opcode byte. If both bytes do not form a supported instruction
     * (for example, if the second byte is another 0xDD/0xFD byte), then {@link IZ80InstructionExecutor.Execute}
     * simply returns; the first 0xDD/0xFD acts then as a NOP, and the
     * second byte will be fetched again for the next invocation of {@link IZ80InstructionExecutor.Execute}.
     * Otherwise, {@link FetchNextOpcode} in invoked in order to get PC properly incremented.
     */
    byte PeekNextOpcode();

    /**
     * Reads one byte from memory.
     *
     * @param address Memory address to read from
     * @return Obtained byte
     */
    byte readFromMemory(short address);

    /**
     * Writes one byte to memory.
     *
     * @param address Memory address to write to
     * @param value Value to write
     */
    void WriteToMemory(short address, byte value);

    /**
     * Reads one byte from an I/O port.
     *
     * @param portNumber Port number to read from
     * @return Obtained byte
     */
    byte ReadFromPort(byte portNumber);

    /**
     * Writes one byte to an I/O port.
     *
     * @param portNumber Port number to write to
     * @param value Value to write
     */
    void WriteToPort(byte portNumber, byte value);

    /**
     * Returns the current register set used by the processor.
     */
    IZ80Registers getRegisters();

    /**
     * Changes the current interrupt mode.
     *
     * @param interruptMode The new interrupt mode.
     * @throws InvalidOperationException">Attempt to set a value other than 0, 1 or 2
     */
    void SetInterruptMode(byte interruptMode);
}
