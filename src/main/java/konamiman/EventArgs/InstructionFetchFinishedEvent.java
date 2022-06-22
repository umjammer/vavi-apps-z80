package konamiman.EventArgs;

import java.util.EventObject;


/**
 * Event args for the {@link IZ80InstructionExecutor.InstructionFetchFinished} event.
 */
public class InstructionFetchFinishedEvent extends EventObject {

    public InstructionFetchFinishedEvent(Object source) {
        super(source);
    }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a return instruction (RET, conditional RET, RETI or RETN)
     */
    protected boolean IsRetInstruction;

    public boolean getIsRetInstruction() { return IsRetInstruction; } public void setIsRetInstruction(boolean value) { IsRetInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a stack load (LD SP,xx) instruction
     */
    protected boolean IsLdSpInstruction; public boolean getIsLdSpInstruction() { return IsLdSpInstruction; } public void setIsLdSpInstruction(boolean value) { IsLdSpInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * a HALT instruction
     */
    protected boolean IsHaltInstruction; public boolean getIsHaltInstruction() { return IsHaltInstruction; } public void setIsHaltInstruction(boolean value) { IsHaltInstruction = value; }

    /**
     * Gets or sets a value that indicates if the instruction that has been executed was
     * an EI instruction or a DI instruction
     */
    private boolean IsEiOrDiInstruction; public boolean getIsEiOrDiInstruction() { return IsEiOrDiInstruction; } public void setIsEiOrDiInstruction(boolean value) { IsEiOrDiInstruction = value; }
}
