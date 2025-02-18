package konamiman.z80.events;

import konamiman.z80.enums.MemoryAccessEventType;
import konamiman.z80.Z80Processor;


/**
 * Event triggered by the {@link Z80Processor} class before and after a memory or port access.
 */
public class MemoryAccessEvent extends ProcessorEvent {

    /**
     * Initializes a new instance of the class.
     *
     * @param eventType          Type of event being processed
     * @param address            Memory or port address being accessed
     * @param value              Initial value for the {@link #value} property
     * @param localUserState     Initial value for the {@link #getLocalUserState()} property
     * @param cancelMemoryAccess Initial value for the {@link #cancelMemoryAccess} property
     */
    public MemoryAccessEvent(
            Object source,
            MemoryAccessEventType eventType,
            short address,
            byte value,
            Object localUserState/*= null*/,
            boolean cancelMemoryAccess/*= false*/)
    {
        super(source);
        this.eventType = eventType;
        this.address = address;
        this.value = value;
        this.localUserState = localUserState;
        this.cancelMemoryAccess = cancelMemoryAccess;
    }

    /**
     * Gets the type of event being processed.
     */
    private final MemoryAccessEventType eventType;

    public MemoryAccessEventType getEventType() { return eventType; }

    /**
     * Gets the memory or port address being accessed.
     */
    private final short address;

    public short getAddress() { return address; }

    /**
     * Gets or sets the value that has been read, will be written, or has been written.
     *
     * <remarks>
     * <para>
     * The underlying memory manager (the {@link Z80Processor#getMemory()}
     * property or the {@link Z80Processor#getPortsSpace()} property of the {@link Z80Processor}
     * that triggered the event) will not be accessed, and the value of this property when the <c>After*</c>
     * event starts will be the same as when the matching <c>Before*</c> method finished
     * (see {@link #eventType}), in the following cases:
     * </para>
     * <list type="bullet">
     * <item><description>The access is a memory or port read, and the memory mode for the address
     * (see {@link Z80Processor#setMemoryAccessMode}, {@link Z80Processor#getMemoryAccessMode},
     * {@link Z80Processor#setPortsSpaceAccessMode}, {@link Z80Processor#getPortAccessMode})
     * is {@link konamiman.z80.enums.MemoryAccessMode#NotConnected} or {@link konamiman.z80.enums.MemoryAccessMode#WriteOnly}.
     * </description></item>
     * <item><description>The access is a memory or port write, and the memory mode for the address
     * is {@link konamiman.z80.enums.MemoryAccessMode#NotConnected} or {@link konamiman.z80.enums.MemoryAccessMode#ReadOnly}.
     * </description></item>
     * <item><description>The {@link #cancelMemoryAccess} property is set to <b>true</b> during
     * the <c>Before*</c> event.
     * </description></item>
     * </list>
     * <para>The value of this property at the beginning of a <c>Before*</c> event for read is always 0xFF.</para>
     */
    private byte value; public byte getValue() { return value; } public void setValue(byte value) { this.value = value; }

    /**
     * Gets or sets a value that indicates whether access to the underlying memory manager should be cancelled.
     *
     * <remarks>
     * <para>
     * If this property is set to <b>true</b> during a <c>Before*</c> event (see {@link #eventType}),
     * then the underlying memory manager (the {@link Z80Processor#getMemory()}
     * property or the {@link Z80Processor#getPortsSpace()} property of the {@link Z80Processor} that triggered the event)
     * will not be accessed. Instead, the matching <c>After*</c> event will be triggered directly, having a
     * {@link #value} equal to the one set in the <c>Before*</c> event.
     * </para>
     * <para>
     * The value of this property when the <c>After*</c> event is triggered is the same that the matching
     * <c>Before*</c> event had when it ended,
     * so it is possible to check whether the memory access was cancelled or not. Changing the value
     * of this property during the <c>After*</c> event has no effect.
     * </para>
     */
    private boolean cancelMemoryAccess; public boolean getCancelMemoryAccess() { return cancelMemoryAccess; } public void setCancelMemoryAccess(boolean value) { cancelMemoryAccess = value; }
}
