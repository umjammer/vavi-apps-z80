package konamiman.EventArgs;

import konamiman.Enums.MemoryAccessEventType;


/**
 * Event triggered by the {@link IZ80Processor} class before and after a memory or port access.
 */
public class MemoryAccessEventArgs extends ProcessorEventArgs {

    /**
     * Initializes a new instance of the class.
     *
     * @param eventType Type of event being processed
     * @param address Memory or port address being accessed
     * @param value Initial value for the {@link Value} property
     * @param localUserState Initial value for the {@link ProcessorEventArgs.LocalUserState} property
     * @param cancelMemoryAccess Initial value for the {@link CancelMemoryAccess} property
     */
    public MemoryAccessEventArgs(
        MemoryAccessEventType eventType,
        short address,
        byte value,
        Object localUserState/*= null*/,
        boolean cancelMemoryAccess/*= false*/)
    {
        super(null); // TODO
        this.EventType = eventType;
        this.Address = address;
        this.Value = value;
        this.setLocalUserState(localUserState);
        this.CancelMemoryAccess = cancelMemoryAccess;
    }

    /**
     * Gets the type of event being processed.
     */
    private MemoryAccessEventType EventType; public MemoryAccessEventType getEventType() { return EventType; }

    /**
     * Gets the memory or port address being accessed.
     */
    private short Address; public short getAddress() { return Address; }

    /**
     * Gets or sets the value that has been read, will be written, or has been written.
     *
     * <remarks>
     * <para>
     * The underlying memory manager (the {@link IZ80Processor.Memory}
     * property or the {@link IZ80Processor.PortsSpace} property of the {@link IZ80Processor}
     * that triggered the event) will not be accessed, and the value of this property when the <c>After*</c>
     * event starts will be the same as when the matching <c>Before*</c> method finished
     * (see {@link EventType}), in the following cases:
     * </para>
     * <list type="bullet">
     * <item><description>The access is a memory or port read, and the memory mode for the address
     * (see {@link IZ80Processor.SetMemoryAccessMode}, {@link IZ80Processor.GetMemoryAccessMode},
     * {@link IZ80Processor.SetPortsSpaceAccessMode}, {@link IZ80Processor.GetPortAccessMode})
     * is {@link MemoryAccessMode.NotConnected} or {@link MemoryAccessMode.WriteOnly}.
     * </description></item>
     * <item><description>The access is a memory or port write, and the memory mode for the address
     * is {@link MemoryAccessMode.NotConnected} or {@link MemoryAccessMode.ReadOnly}.
     * </description></item>
     * <item><description>The {@link CancelMemoryAccess} property is set to <b>true</b> during
     * the <c>Before*</c> event.
     * </description></item>
     * </list>
     * <para>The value of this property at the beginning of a <c>Before*</c> event for read is always 0xFF.</para>
     */
    private byte Value; public byte getValue() { return Value; } public void setValue(byte value) { Value = value; }

    /**
     * Gets or sets a value that indicates whether access to the underlying memory manager should be cancelled.
     *
     * <remarks>
     * <para>
     * If this property is set to <b>true</b> during a <c>Before*</c> event (see {@link EventType}),
     * then the underlying memory manager (the {@link IZ80Processor.Memory}
     * property or the {@link IZ80Processor.PortsSpace} property of the {@link IZ80Processor} that triggered the event)
     * will not be accessed. Instead, the matching <c>After*</c> event will be triggered directly, having a
     * {@link Value} equal to the one set in the <c>Before*</c> event.
     * </para>
     * <para>
     * The value of this property when the <c>After*</c> event is triggered is the same that the matching
     * <c>Before*</c> event had when it ended,
     * so it is possible to check whether the memory access was cancelled or not. Changing the value
     * of this property during the <c>After*</c> event has no effect.
     * </para>
     */
    private boolean CancelMemoryAccess; public boolean getCancelMemoryAccess() { return CancelMemoryAccess; } public void setCancelMemoryAccess(boolean value) { CancelMemoryAccess = value; }
}
