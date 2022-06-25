package konamiman.z80.utils;

import java.util.ArrayList;
import java.util.List;

import konamiman.z80.enums.StopReason;


/**
 * Internal class used to keep track of the current instruction execution.
 */
public class InstructionExecutionContext {

    public InstructionExecutionContext() {
        stopReason = StopReason.NotApplicable;
        opcodeBytes = new ArrayList<>();
    }

    private StopReason stopReason;

    public StopReason getStopReason() {
        return stopReason;
    }

    public void setStopReason(StopReason value) {
        stopReason = value;
    }

    private boolean mustStop;
    public boolean getMustStop()
        {
            return stopReason != StopReason.NotApplicable;
        }

    public void startNewInstruction() {
        opcodeBytes.clear();
        fetchComplete = false;
        localUserStateFromPreviousEvent = null;
        accummulatedMemoryWaitStates = 0;
        peekedOpcode = null;
        isEiOrDiInstruction = false;
    }

    private boolean executingBeforeInstructionEvent;

    public boolean isExecutingBeforeInstructionEvent() {
        return executingBeforeInstructionEvent;
    }

    public void setExecutingBeforeInstructionEvent(boolean value) {
        executingBeforeInstructionEvent = value;
    }

    private boolean fetchComplete;

    public boolean isFetchComplete() {
        return fetchComplete;
    }

    public void setFetchComplete(boolean value) {
        fetchComplete = value;
    }

    private List<Byte> opcodeBytes;

    public List<Byte> getOpcodeBytes() {
        return opcodeBytes;
    }

    public void setOpcodeBytes(List<Byte> value) {
        opcodeBytes = value;
    }

    private boolean isRetInstruction;

    public boolean isRetInstruction() {
        return isRetInstruction;
    }

    public void setRetInstruction(boolean value) {
        isRetInstruction = value;
    }

    private boolean isLdSpInstruction;

    public boolean isLdSpInstruction() {
        return isLdSpInstruction;
    }

    public void setLdSpInstruction(boolean value) {
        isLdSpInstruction = value;
    }

    private boolean isHaltInstruction;

    public boolean isHaltInstruction() {
        return isHaltInstruction;
    }

    public void setHaltInstruction(boolean value) {
        isHaltInstruction = value;
    }

    private boolean isEiOrDiInstruction;

    public boolean isEiOrDiInstruction() {
        return isEiOrDiInstruction;
    }

    public void setEiOrDiInstruction(boolean value) {
        isEiOrDiInstruction = value;
    }

    private short spAfterInstructionFetch;

    public short getSpAfterInstructionFetch() {
        return spAfterInstructionFetch;
    }

    public void setSpAfterInstructionFetch(short value) {
        spAfterInstructionFetch = value;
    }

    private Object localUserStateFromPreviousEvent;

    public Object getLocalUserStateFromPreviousEvent() {
        return localUserStateFromPreviousEvent;
    }

    public void setLocalUserStateFromPreviousEvent(Object value) {
        localUserStateFromPreviousEvent = value;
    }

    private int accummulatedMemoryWaitStates;

    public int getAccummulatedMemoryWaitStates() {
        return accummulatedMemoryWaitStates;
    }

    public void setAccummulatedMemoryWaitStates(int value) {
        accummulatedMemoryWaitStates = value;
    }

    private Byte peekedOpcode;

    public Byte getPeekedOpcode() {
        return peekedOpcode;
    }

    /** @param value nullable */
    public void setPeekedOpcode(Byte value) {
        peekedOpcode = value;
    }

    private short addressOfPeekedOpcode;

    public short getAddressOfPeekedOpcode() {
        return addressOfPeekedOpcode;
    }

    public void setAddressOfPeekedOpcode(short value) {
        addressOfPeekedOpcode = value;
    }
}

