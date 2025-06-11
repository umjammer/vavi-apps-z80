package konamiman.z80;

import konamiman.z80.enums.MemoryAccessMode;


/**
 * Interface for implementations of Z80Processor that support extended (16 bits) ports space.
 */
public interface Z80ProcessorExtendedPortsSpace {

    /**
     * Gets a value indicating whether the processor is using extended (16 bits) ports space.
     */
    boolean getUseExtendedPortsSpace();

    /**
     * Sets a value indicating whether the processor is using extended (16 bits) ports space.
     */
    void setUseExtendedPortsSpace(boolean value);

    /**
     * Sets the access mode of a portion of the visible ports space, specifying the initial port as a 16 bit number.
     *
     * @param startPort First port that will be set
     * @param length    Length of the mports space that will be set
     * @param mode      New memory mode
     * @throws IllegalArgumentException {@code startAddress} is less than 0,
     *                                  or {@code startAddress} + {@code length} goes beyond 255.
     */
    void setExtendedPortsSpaceAccessMode(short startPort, int length, MemoryAccessMode mode);

    /**
     * Gets the access mode of a port, specifying the port as a 16 bit number.
     *
     * @param portNumber The port number to check
     * @return The current memory access mode for the port
     * @throws IllegalArgumentException {@code portNumber} is greater than 255.
     */
    MemoryAccessMode getExtendedPortAccessMode(short portNumber);

    /**
     * Sets the wait states that will be simulated when accessing the I/O ports,
     * specifying the initial port as a 16 bit number.
     *
     * @param startPort  First port that will be configured
     * @param length     Length of the port range that will be configured
     * @param waitStates New wait states
     * @throws IllegalStateException {@code startAddress} + {@code length} goes beyond 255.
     */
    void setExtendedPortWaitStates(short startPort, int length, byte waitStates);

    /**
     * Obtains the wait states that will be simulated when accessing the I/O ports
     * for a given port, specifying the port as a 16 bit number.
     *
     * @param portNumber Port number to het the wait states for
     * @return Current wait states for the specified port
     */
    byte getExtendedPortWaitStates(short portNumber);
}
