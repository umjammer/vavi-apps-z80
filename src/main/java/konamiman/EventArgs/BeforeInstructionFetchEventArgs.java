package konamiman.EventArgs;

import konamiman.DependenciesInterfaces.IExecutionStopper;


/**
 * Event triggered by the {@link IZ80Processor} class before an instruction is fetched.
 */
public class BeforeInstructionFetchEventArgs extends ProcessorEventArgs {

    /**
     * Initializes a new instance of the class.
     *
     * @param stopper An instance of {@link IExecutionStopper} that can be used
     * by the event listener to request stop of the execution loop.
     */
    public BeforeInstructionFetchEventArgs(IExecutionStopper stopper)
    {
        super(stopper);
        this.ExecutionStopper = stopper;
    }

    /**
     * Contains the instance of {@link IExecutionStopper} that allows the event consumer
     * to ask termination of the processor execution.
     */
    private IExecutionStopper ExecutionStopper; public IExecutionStopper getExecutionStopper() { return ExecutionStopper; }
}
