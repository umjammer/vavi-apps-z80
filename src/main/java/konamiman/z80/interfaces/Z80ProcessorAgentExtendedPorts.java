package konamiman.z80.interfaces;

/**
 * Complements {@link Z80ProcessorAgent} with methods for accessing the extended (16 bits) ports space.
 */
public interface Z80ProcessorAgentExtendedPorts {

    /**
     * Reads one byte from an I/O port, specifying the port as a 16 bit number.
     *
     * @param portNumberLow  Port number to read from (low byte)
     * @param portNumberHigh Port number to read from (high byte)
     * @return Obtained byte
     */
    byte readFromPort(byte portNumberLow, byte portNumberHigh);

    /**
     * Writes one byte to an I/O port, specifying the port as a 16 bit number.
     *
     * @param portNumberLow  Port number to write to (low byte)
     * @param portNumberHigh Port number to write to (high byte)
     * @param value          Value to write
     */
    void writeToPort(byte portNumberLow, byte portNumberHigh, byte value);
}
