package konamiman.z80.interfaces;


import konamiman.z80.events.InstructionFetchFinishedEvent;
import dotnet4j.util.compat.EventHandler;


/**
 * Represents a class that can execute Z80 instructions.
 */
public interface Z80InstructionExecutor {

    /**
     * The {@link Z80ProcessorAgent} that the {@link #execute(byte)} method will use in order
     * to interact with the processor.
     */
    Z80ProcessorAgent getProcessorAgent(); void setProcessorAgent(Z80ProcessorAgent value);

    /**
     * Executes the next instruction.
     *
     * @param firstOpcodeByte First byte of the opcode for the instruction to execute
     * @return Total number of T states elapsed when executing the instruction,
     * not including extra memory wait states.
     *
     * <remarks>
     * <para>
     * The execution flow for this method should be as follows:
     * </para>
     * <list type="number">
     * <item><description>If needed, extra opcode bytes are fetched by using the
     * {@link Z80ProcessorAgent#fetchNextOpcode()} method on {@link #getProcessorAgent()}.</description></item>
     * <item><description>The {@link #instructionFetchFinished()} event is triggered.</description></item>
     * <item><description>The instruction is processed by accessing the {@link #getProcessorAgent()} members as appropriate.</description></item>
     * <item><description>The method terminates, returning the total count of T states required for the instruction
     * execution, not including any extra memory or port wait states (but including the automatically
     * inserted wait state used for port access).</description></item>
     * </list>
     * <para>
     * The PC register will point to the address after the supplied opcode byte when this method is invoked,
     * and each subsequent call to {@link Z80ProcessorAgent#fetchNextOpcode()} will further increment
     * PC by one. This has to be taken in account when implementing the relative jump instructions (DJNZ and JR).
     * </para>
     */
    int execute(byte firstOpcodeByte);

    /**
     * Event triggered when the instruction opcode has been fully fetched.
     */
    EventHandler<InstructionFetchFinishedEvent> instructionFetchFinished();
}
