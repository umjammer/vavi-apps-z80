package konamiman;

import konamiman.DataTypesAndUtils.InstructionExecutionContext;


public class Z80ProcessorForTests extends Z80Processor {

    public void SetInstructionExecutionContextToNonNull() {
        executionContext = new InstructionExecutionContext();
    }

    public void SetInstructionExecutionContextToNull() {
        executionContext = null;
    }

    private boolean MustFailIfNoInstructionFetchComplete;

    public boolean getMustFailIfNoInstructionFetchComplete() {
        return MustFailIfNoInstructionFetchComplete;
    }

    public void setMustFailIfNoInstructionFetchComplete(boolean value) {
        MustFailIfNoInstructionFetchComplete = value;
    }

    protected @Override void FailIfNoInstructionFetchComplete() {
        if (MustFailIfNoInstructionFetchComplete)
            super.FailIfNoInstructionFetchComplete();
    }

    public void SetStartOFStack(short value) {
        startOfStack = value;
    }

    public void SetIsHalted() {
        isHalted = true;
    }
}
