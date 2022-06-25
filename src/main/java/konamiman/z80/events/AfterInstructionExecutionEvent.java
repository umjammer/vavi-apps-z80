package konamiman.z80.events;


import konamiman.z80.interfaces.ExecutionStopper;
import konamiman.z80.Z80Processor;


/**
 * Event args for the event triggered by the {@link Z80Processor} class after an instruction is executed.
 */
public class AfterInstructionExecutionEvent extends ProcessorEvent {

    /**
     * Initializes a new instance of the class.
     *
     * @param opcode         The opcode bytes of the instruction that has been executed.
     * @param stopper        An instance of {@link ExecutionStopper} that can be used
     *                       by the event listener to request stop of the execution loop.
     * @param localUserState The state object from the matching {@link Z80Processor#beforeInstructionExecution()} event.
     * @param tStates        Total count of T states used for the instruction execution, including extra wait states
     */
    public AfterInstructionExecutionEvent(Object source, byte[] opcode, konamiman.z80.interfaces.ExecutionStopper stopper, Object localUserState, int tStates) {
        super(source);
        this.opcode = opcode;
        this.executionStopper = stopper;
        this.localUserState = localUserState;
        this.totalTStates = tStates;
    }

    /**
     * Contains the full opcode bytes of the instruction that has been executed.
     */
    private byte[] opcode; public byte[] getOpcode() { return opcode; } public void setOpcode(byte[] value) { opcode = value; }

    /**
     * Contains the instance of {@link ExecutionStopper} that allows the event consumer
     * to ask termination of the processor execution.
     */
    private ExecutionStopper executionStopper; public ExecutionStopper getExecutionStopper() { return executionStopper; }

    /**
     * Contains the total count of T states required for the instruction execution, including
     * any extra wait states used for memory and ports access.
     */
    private int totalTStates; public int getTotalTStates() { return totalTStates; }
}
