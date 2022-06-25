package konamiman.z80.events;

import java.util.EventObject;

import konamiman.z80.interfaces.Z80InstructionExecutor;


/**
 * Event args for the {@link Z80InstructionExecutor#instructionFetchFinished()} event.
 */
public class InstructionFetchFinishedEvent extends EventObject {

    public InstructionFetchFinishedEvent(Object source) {
        super(source);
    }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a return instruction (RET, conditional RET, RETI or RETN)
     */
    protected boolean isRetInstruction;

    public boolean isRetInstruction() { return isRetInstruction; } public void setRetInstruction(boolean value) { isRetInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a stack load (LD SP,xx) instruction
     */
    protected boolean isLdSpInstruction; public boolean isLdSpInstruction() { return isLdSpInstruction; } public void setLdSpInstruction(boolean value) { isLdSpInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a HALT instruction
     */
    protected boolean isHaltInstruction; public boolean isHaltInstruction() { return isHaltInstruction; } public void setHaltInstruction(boolean value) { isHaltInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * an EI instruction or a DI instruction
     */
    private boolean isEiOrDiInstruction; public boolean isEiOrDiInstruction() { return isEiOrDiInstruction; } public void setEiOrDiInstruction(boolean value) { isEiOrDiInstruction = value; }
}
