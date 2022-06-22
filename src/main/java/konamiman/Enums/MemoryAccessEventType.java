package konamiman.Enums;

/**
 * Represents the type of a memory access event.
 */
public enum MemoryAccessEventType {
    /**
     * A memory address is going to be read.
     */
    BeforeMemoryRead,

    /**
     * A memory address has been read.
     */
    AfterMemoryRead,

    /**
     * A memory address is going to be written.
     */
    BeforeMemoryWrite,

    /**
     * A memory address has been written.
     */
    AfterMemoryWrite,

    /**
     * A port is going to be read.
     */
    BeforePortRead,

    /**
     * A port has been read.
     */
    AfterPortRead,

    /**
     * A port is going to be written.
     */
    BeforePortWrite,

    /**
     * A port has been written.
     */
    AfterPortWrite
}
