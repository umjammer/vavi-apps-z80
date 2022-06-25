package konamiman.z80;

import konamiman.z80.utils.InstructionExecutionContext;


public class Z80ProcessorForTests extends Z80ProcessorImpl {

    public void setInstructionExecutionContextToNonNull() {
        executionContext = new InstructionExecutionContext();
    }

    public void setInstructionExecutionContextToNull() {
        executionContext = null;
    }

    private boolean mustFailIfNoInstructionFetchComplete;

    public void setMustFailIfNoInstructionFetchComplete(boolean value) {
        mustFailIfNoInstructionFetchComplete = value;
    }

    @Override protected void failIfNoInstructionFetchComplete() {
        if (mustFailIfNoInstructionFetchComplete)
            super.failIfNoInstructionFetchComplete();
    }

    public void setStartOfStack(short value) {
        startOfStack = value;
    }

    public void setHalted() {
        isHalted = true;
    }
}
