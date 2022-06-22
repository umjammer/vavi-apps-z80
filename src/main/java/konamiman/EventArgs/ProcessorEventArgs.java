package konamiman.EventArgs;

import java.util.EventObject;


/**
 * Base class for all the events triggered by the {@link IZ80Processor} class.
 */
public abstract class ProcessorEventArgs extends EventObject {

    public ProcessorEventArgs(Object source) {
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
    private Object LocalUserState;

    public Object getLocalUserState() { return LocalUserState; } public void setLocalUserState(Object value) { LocalUserState = value; }
}
