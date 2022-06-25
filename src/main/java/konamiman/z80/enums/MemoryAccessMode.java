package konamiman.z80.enums;

import konamiman.z80.interfaces.Memory;


/**
 * Represents the access mode for a certain memory address.
 */
public enum MemoryAccessMode {
    /**
     * Read and write. The registered {@link Memory} object will be accessed
     * for both reading and writing from and to memory.
     */
    ReadAndWrite,

    /**
     * Read only. The registered {@link Memory} object will be accessed
     * only for reading to memory.
     */
    ReadOnly,

    /**
     * Write only. The registered {@link Memory} object will be accessed
     * only for writing to memory. The read value will always be FFh.
     */
    WriteOnly,

    /**
     * Not connected. The registered {@link Memory} object will never be accessed
     * for the affected memory address. The read value will always be FFh.
     */
    NotConnected
}
