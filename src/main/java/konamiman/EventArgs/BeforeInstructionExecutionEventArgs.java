package konamiman.EventArgs;


/**
 * Event triggered by the {@link IZ80Processor} class before an instruction is executed.
 */
public class BeforeInstructionExecutionEventArgs extends ProcessorEventArgs {

    /**
     * Initializes a new instance of the class.
     *
     * @param opcode The full opcode bytes of the instruction that is about to be executed.
     */
    public BeforeInstructionExecutionEventArgs(byte[] opcode, Object localUserState)
    {
        super(localUserState);
        this.Opcode = opcode;
        this.setLocalUserState(localUserState);
    }

    /**
     * Contains the full opcode bytes of the instruction that is about to be executed.
     */
    private byte[] Opcode; public byte[] getOpcode() { return Opcode; } public void setOpcode(byte[] value) { Opcode = value; }
}
