package konamiman.DataTypesAndUtils;

import java.util.ArrayList;
import java.util.List;

import konamiman.Enums.StopReason;


/**
 * Internal class used to keep track of the current instruction execution.
 */
public class InstructionExecutionContext {
    public InstructionExecutionContext() {
        _StopReason = StopReason.NotApplicable;
        OpcodeBytes = new ArrayList<>();
    }

    private konamiman.Enums.StopReason _StopReason;

    public StopReason getStopReason() {
        return _StopReason;
    }

    public void setStopReason(StopReason value) {
        _StopReason = value;
    }

    private boolean MustStop;
    public boolean getMustStop()
        {
            return _StopReason != StopReason.NotApplicable;
        }

    public void StartNewInstruction() {
        OpcodeBytes.clear();
        FetchComplete = false;
        LocalUserStateFromPreviousEvent = null;
        AccummulatedMemoryWaitStates = 0;
        PeekedOpcode = null;
        IsEiOrDiInstruction = false;
    }

    private boolean ExecutingBeforeInstructionEvent;

    public boolean getExecutingBeforeInstructionEvent() {
        return ExecutingBeforeInstructionEvent;
    }

    public void setExecutingBeforeInstructionEvent(boolean value) {
        ExecutingBeforeInstructionEvent = value;
    }

    private boolean FetchComplete;

    public boolean getFetchComplete() {
        return FetchComplete;
    }

    public void setFetchComplete(boolean value) {
        FetchComplete = value;
    }

    private List<Byte> OpcodeBytes;

    public List<Byte> getOpcodeBytes() {
        return OpcodeBytes;
    }

    public void setOpcodeBytes(List<Byte> value) {
        OpcodeBytes = value;
    }

    private boolean IsRetInstruction;

    public boolean getIsRetInstruction() {
        return IsRetInstruction;
    }

    public void setIsRetInstruction(boolean value) {
        IsRetInstruction = value;
    }

    private boolean IsLdSpInstruction;

    public boolean getIsLdSpInstruction() {
        return IsLdSpInstruction;
    }

    public void setIsLdSpInstruction(boolean value) {
        IsLdSpInstruction = value;
    }

    private boolean IsHaltInstruction;

    public boolean getIsHaltInstruction() {
        return IsHaltInstruction;
    }

    public void setIsHaltInstruction(boolean value) {
        IsHaltInstruction = value;
    }

    private boolean IsEiOrDiInstruction;

    public boolean getIsEiOrDiInstruction() {
        return IsEiOrDiInstruction;
    }

    public void setIsEiOrDiInstruction(boolean value) {
        IsEiOrDiInstruction = value;
    }

    private short SpAfterInstructionFetch;

    public short getSpAfterInstructionFetch() {
        return SpAfterInstructionFetch;
    }

    public void setSpAfterInstructionFetch(short value) {
        SpAfterInstructionFetch = value;
    }

    private Object LocalUserStateFromPreviousEvent;

    public Object getLocalUserStateFromPreviousEvent() {
        return LocalUserStateFromPreviousEvent;
    }

    public void setLocalUserStateFromPreviousEvent(Object value) {
        LocalUserStateFromPreviousEvent = value;
    }

    private int AccummulatedMemoryWaitStates;

    public int getAccummulatedMemoryWaitStates() {
        return AccummulatedMemoryWaitStates;
    }

    public void setAccummulatedMemoryWaitStates(int value) {
        AccummulatedMemoryWaitStates = value;
    }

    private Byte PeekedOpcode;

    public Byte getPeekedOpcode() {
        return PeekedOpcode;
    }

    public void setPeekedOpcode(Byte value) {
        PeekedOpcode = value;
    }

    private short AddressOfPeekedOpcode;

    public short getAddressOfPeekedOpcode() {
        return AddressOfPeekedOpcode;
    }

    public void setAddressOfPeekedOpcode(short value) {
        AddressOfPeekedOpcode = value;
    }
}

