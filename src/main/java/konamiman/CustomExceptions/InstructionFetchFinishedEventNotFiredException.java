package konamiman.CustomExceptions;

/**
 * Exception that is thrown by {@link IZ80Processor} when the {@link IZ80InstructionExecutor.Execute}
 * method of the {@link IZ80InstructionExecutor} instance used returns without having fired the
 * {@link IZ80InstructionExecutor.InstructionFetchFinished} event.
 */
public class InstructionFetchFinishedEventNotFiredException extends RuntimeException {
    /**
     * The memory address of the first opcode byte fetched for the instruction that caused the exception.
     */
    private short InstructionAddress;

    public short getInstructionAddress() {
        return InstructionAddress;
    }

    public void setInstructionAddress(short value) {
        InstructionAddress = value;
    }

    /**
     * The opcode bytes of the instruction that caused the exception.
     */
    private byte[] FetchedBytes;

    public byte[] getFetchedBytes() {
        return FetchedBytes;
    }

    public void setFetchedBytes(byte[] value) {
        FetchedBytes = value;
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
        super(message != null ? message : "IZ80InstructionExecutor.Execute returned without having fired the InstructionFetchFinished event.", innerException);

        this.InstructionAddress = instructionAddress;
        this.FetchedBytes = fetchedBytes;
    }
}
