package konamiman.z80.exceptions;

import konamiman.z80.interfaces.Z80InstructionExecutor;
import konamiman.z80.Z80Processor;


/**
 * Exception that is thrown by {@link Z80Processor} when the {@link Z80InstructionExecutor#execute(byte)}
 * method of the {@link Z80InstructionExecutor} instance used returns without having fired the
 * {@link Z80InstructionExecutor#instructionFetchFinished()} event.
 */
public class InstructionFetchFinishedEventNotFiredException extends RuntimeException {
    /**
     * The memory address of the first opcode byte fetched for the instruction that caused the exception.
     */
    private short instructionAddress;

    public short getInstructionAddress() {
        return instructionAddress;
    }

    public void setInstructionAddress(short value) {
        instructionAddress = value;
    }

    /**
     * The opcode bytes of the instruction that caused the exception.
     */
    private byte[] fetchedBytes;

    public byte[] getFetchedBytes() {
        return fetchedBytes;
    }

    public void setFetchedBytes(byte[] value) {
        fetchedBytes = value;
    }

    /**
     * Initializes a new instance of the class.
     *
     * @param instructionAddress The memory address of the first opcode byte fetched for the instruction that caused the exception.
     * @param fetchedBytes The opcode bytes of the instruction that caused the exception.
     * @param message Message for the exception.
     * @param innerException Inner exception.
     */
    public InstructionFetchFinishedEventNotFiredException(
            short instructionAddress,
            byte[] fetchedBytes,
            String message/*= null*/,
            Exception innerException/*= null*/) {
        super(message != null ? message : "Z80InstructionExecutorImpl.Execute returned without having fired the InstructionFetchFinished event.", innerException);

        this.instructionAddress = instructionAddress;
        this.fetchedBytes = fetchedBytes;
    }
}
