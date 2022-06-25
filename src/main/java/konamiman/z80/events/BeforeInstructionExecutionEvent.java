package konamiman.z80.events;


import konamiman.z80.Z80Processor;


/**
 * Event triggered by the {@link Z80Processor} class before an instruction is executed.
 */
public class BeforeInstructionExecutionEvent extends ProcessorEvent {

    /**
     * Initializes a new instance of the class.
     *
     * @param opcode The full opcode bytes of the instruction that is about to be executed.
     */
    public BeforeInstructionExecutionEvent(Object source, byte[] opcode, Object localUserState) {
        super(source);
        this.opcode = opcode;
        this.localUserState = localUserState;
    }

    /**
     * Contains the full opcode bytes of the instruction that is about to be executed.
     */
    private byte[] opcode; public byte[] getOpcode() { return opcode; } public void setOpcode(byte[] value) { opcode = value; }
}
