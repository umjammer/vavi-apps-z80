package konamiman.z80.events;

import java.util.EventObject;

import konamiman.z80.Z80Processor;


/**
 * Base class for all the events triggered by the {@link Z80Processor} class.
 */
public abstract class ProcessorEvent extends EventObject {

    public ProcessorEvent(Object source) {
        super(source);
    }

    /**
     * User-defined state object that is passed from the <c>Before*</c> events to the matching
     * <c>After*</c> events.
     *
     * <remarks>
     * This property is always null at the beginning of the <c>Before*</c> events.
     * The client code can set this property to any value in these events, and the value
     * will be replicated in the same property of the corresponding <c>After*</c> event.
     */
    protected Object localUserState;

    public Object getLocalUserState() { return localUserState; } public void setLocalUserState(Object value) { localUserState = value; }
}
