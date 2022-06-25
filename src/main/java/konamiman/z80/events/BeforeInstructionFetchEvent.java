package konamiman.z80.events;

import konamiman.z80.interfaces.ExecutionStopper;
import konamiman.z80.Z80Processor;


/**
 * Event triggered by the {@link Z80Processor} class before an instruction is fetched.
 */
public class BeforeInstructionFetchEvent extends ProcessorEvent {

    /**
     * Initializes a new instance of the class.
     *
     * @param stopper An instance of {@link ExecutionStopper} that can be used
     *                by the event listener to request stop of the execution loop.
     */
    public BeforeInstructionFetchEvent(Object source, ExecutionStopper stopper) {
        super(source);
        this.executionStopper = stopper;
    }

    /**
     * Contains the instance of {@link ExecutionStopper} that allows the event consumer
     * to ask termination of the processor execution.
     */
    private ExecutionStopper executionStopper; public ExecutionStopper getExecutionStopper() { return executionStopper; }
}
