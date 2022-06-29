package konamiman.z80.interfaces;

import java.util.Optional;

import dotnet4j.util.compat.EventHandler;


/**
 * Represents a source of interrupts for a Z80 processor.
 */
public interface Z80InterruptSource {
    /**
     * Signals a NMI interrupt pulse generated by the source.
     */
    EventHandler<?> nmiInterruptPulse();

    /**
     * Gets the current state of the INT line as set by the source.
     * This is a logical state: a value of <i>True</i> means that an interrupt is requested.
     */
    boolean isIntLineIsActive();

    /**
     * Gets the value put on the data bus by the source. This is used by the processor in interrupt mode 2 only.
     */
    Optional<Byte> getValueOnDataBus();
}

