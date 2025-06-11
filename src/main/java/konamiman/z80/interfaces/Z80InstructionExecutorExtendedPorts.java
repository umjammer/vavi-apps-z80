package konamiman.z80.interfaces;

/**
 * Complements {@link Z80InstructionExecutor} with a property of type {@link Z80ProcessorAgentExtendedPorts}
 */
public interface Z80InstructionExecutorExtendedPorts {

    /**
     * The {@link Z80ProcessorAgentExtendedPorts} that the {@link Z80ProcessorAgentExtendedPorts#execute} method will use in order
     * to interact with the processor.
     */
    Z80ProcessorAgentExtendedPorts getProcessorAgentExtendedPorts();

    void setProcessorAgentExtendedPorts(Z80ProcessorAgentExtendedPorts value);
}
