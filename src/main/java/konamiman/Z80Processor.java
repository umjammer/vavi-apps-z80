package konamiman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import konamiman.CustomExceptions.InstructionFetchFinishedEventNotFiredException;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.InstructionExecutionContext;
import konamiman.DataTypesAndUtils.NumberUtils;
import konamiman.DependenciesImplementations.ClockSynchronizer;
import konamiman.DependenciesImplementations.PlainMemory;
import konamiman.DependenciesImplementations.Z80Registers;
import konamiman.DependenciesInterfaces.IClockSynchronizer;
import konamiman.DependenciesInterfaces.IMemory;
import konamiman.DependenciesInterfaces.IZ80InstructionExecutor;
import konamiman.DependenciesInterfaces.IZ80InterruptSource;
import konamiman.DependenciesInterfaces.IZ80ProcessorAgent;
import konamiman.DependenciesInterfaces.IZ80Registers;
import konamiman.Enums.MemoryAccessEventType;
import konamiman.Enums.MemoryAccessMode;
import konamiman.Enums.ProcessorState;
import konamiman.Enums.StopReason;
import konamiman.EventArgs.AfterInstructionExecutionEventArgs;
import konamiman.EventArgs.BeforeInstructionExecutionEventArgs;
import konamiman.EventArgs.BeforeInstructionFetchEventArgs;
import konamiman.EventArgs.InstructionFetchFinishedEvent;
import konamiman.EventArgs.MemoryAccessEventArgs;
import konamiman.InstructionsExecution.Core.Z80InstructionExecutor;
import vavi.util.dotnet.EventHandler;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;


/**
 * The implementation of the {@link IZ80Processor} interface.
 */
public class Z80Processor implements IZ80Processor, IZ80ProcessorAgent {

    private static final int MemorySpaceSize = 65536;
    private static final int PortSpaceSize = 256;

    private static final long MaxEffectiveClockSpeed = 100;
    private static final double MinEffectiveClockSpeed = 0.001;

    private static final short NmiServiceRoutine = 0x66;
    private static final byte NOP_opcode = 0x00;
    private static final byte RST38h_opcode = (byte) 0xFF;

    public Z80Processor()
    {
        clockSynchronizer = new ClockSynchronizer();

        _ClockFrequencyInMHz = 4;
        _ClockSpeedFactor = 1;

        AutoStopOnDiPlusHalt = true;
        AutoStopOnRetWithStackEmpty = false;
        startOfStack =  (short)0xFFFF;

        SetMemoryWaitStatesForM1((short) 0, MemorySpaceSize, (byte) 0);
        SetMemoryWaitStatesForNonM1((short) 0, MemorySpaceSize, (byte) 0);
        SetPortWaitStates((short) 0, PortSpaceSize, (byte)0);

        _Memory = new PlainMemory(MemorySpaceSize);
        _PortsSpace = new PlainMemory(PortSpaceSize);

        SetMemoryAccessMode((short) 0, MemorySpaceSize, MemoryAccessMode.ReadAndWrite);
        SetPortsSpaceAccessMode((byte) 0, PortSpaceSize, MemoryAccessMode.ReadAndWrite);

        _Registers = new Z80Registers();
        _InterruptSources = new ArrayList<>();

        _InstructionExecutor = new Z80InstructionExecutor();

        _StopReason = StopReason.NeverRan;
        State = ProcessorState.Stopped;
    }

//#region Processor control

    public void Start(Object userState/*= null*/)
    {
        if(userState != null)
            this.UserState = userState;

        Reset();
        TStatesElapsedSinceStart = 0;

        InstructionExecutionLoop(false);
    }

    public void Continue()
    {
        InstructionExecutionLoop(false);
    }

    private int InstructionExecutionLoop(boolean isSingleInstruction/*= false*/)
    {
        try
        {
            return InstructionExecutionLoopCore(isSingleInstruction);
        }
        catch(Exception e)
        {
            State = ProcessorState.Stopped;
            _StopReason = StopReason.ExceptionThrown;

            throw e;
        }
    }

    private int InstructionExecutionLoopCore(boolean isSingleInstruction)
    {
        if(clockSynchronizer != null) clockSynchronizer.Start();
        executionContext = new InstructionExecutionContext();
        _StopReason = StopReason.NotApplicable;
        State = ProcessorState.Running;
        var totalTStates = 0;

        while(!executionContext.getMustStop())
        {
            executionContext.StartNewInstruction();

            FireBeforeInstructionFetchEvent();
            if(executionContext.getMustStop())
                break;

            var executionTStates = ExecuteNextOpcode();

            totalTStates = executionTStates + executionContext.getAccummulatedMemoryWaitStates();
            TStatesElapsedSinceStart += totalTStates;
            TStatesElapsedSinceReset += totalTStates;

            ThrowIfNoFetchFinishedEventFired();

            if(!isSingleInstruction)
            {
                CheckAutoStopForHaltOnDi();
                CheckForAutoStopForRetWithStackEmpty();
                CheckForLdSpInstruction();
            }

            FireAfterInstructionExecutionEvent(totalTStates);

            if(!isHalted)
                isHalted = executionContext.getIsHaltInstruction();

            var interruptTStates = AcceptPendingInterrupt();
            totalTStates += interruptTStates;
            TStatesElapsedSinceStart += interruptTStates;
            TStatesElapsedSinceReset += interruptTStates;

            if(isSingleInstruction)
                executionContext.setStopReason(StopReason.ExecuteNextInstructionInvoked);
            else if(clockSynchronizer != null)
                clockSynchronizer.TryWait(totalTStates);
        }

        if(clockSynchronizer != null)
            clockSynchronizer.Stop();
        this._StopReason = executionContext.getStopReason();
        this.State =
            _StopReason == StopReason.PauseInvoked
                ? ProcessorState.Paused
                : ProcessorState.Stopped;

        executionContext = null;

        return totalTStates;
    }

    private int ExecuteNextOpcode()
    {
        if (isHalted) {
            executionContext.getOpcodeBytes().add(NOP_opcode);
            return _InstructionExecutor.execute(NOP_opcode);
        }

        return _InstructionExecutor.execute(fetchNextOpcode());
    }

    private int AcceptPendingInterrupt()
    {
        if(executionContext.getIsEiOrDiInstruction())
            return 0;

        if (_nmiInterruptPending) {
            isHalted = false;
            _Registers.setIFF1(Bit.of(0));
            ExecuteCall(NmiServiceRoutine);
            return 11;
        }

        if(!getInterruptsEnabled())
            return 0;

        var activeIntSource = _InterruptSources.size() > 0 ? _InterruptSources.get(0) : null /* TODO getIntLineIsActive */;
        if (activeIntSource == null)
            return 0;

        _Registers.setIFF1(Bit.of(0));
        _Registers.setIFF2(Bit.of(0));
        isHalted = false;

        switch(_InterruptMode) {
        case 0:
            var opcode = activeIntSource.getValueOnDataBus().orElse((byte) 0xFF);
            _InstructionExecutor.execute(opcode);
            return 13;
        case 1:
            _InstructionExecutor.execute(RST38h_opcode);
            return 13;
        case 2:
            var pointerAddress = NumberUtils.CreateUshort(
                /*lowByte:*/ activeIntSource.getValueOnDataBus().orElse((byte) 0xFF),
                /*highByte:*/ _Registers.getI());
            var callAddress = NumberUtils.CreateUshort(
                /*lowByte:*/ ReadFromMemoryInternal(pointerAddress),
                /*highByte:*/ ReadFromMemoryInternal((short)(pointerAddress + 1)));
            ExecuteCall(callAddress);
            return 19;
        }

        return 0;
    }

    public void ExecuteCall(short address)
    {
        var oldAddress = _Registers.getPC();
        var sp = (short)(_Registers.getSP() - 1);
        WriteToMemoryInternal(sp, GetHighByte(oldAddress));
        sp = (short)(sp - 1);
        WriteToMemoryInternal(sp, GetLowByte(oldAddress));

        _Registers.setSP(sp);
        _Registers.setPC(address);
    }

    public void ExecuteRet()
    {
        var sp = _Registers.getSP();
        var newPC = NumberUtils.createShort(ReadFromMemoryInternal(sp), ReadFromMemoryInternal((short)(sp + 1)));

        _Registers.setPC(newPC);
        _Registers.setPC((short) (_Registers.getPC() + 2));
    }

    private void ThrowIfNoFetchFinishedEventFired() throws InstructionFetchFinishedEventNotFiredException {
        if (executionContext.getFetchComplete())
            return;

        throw new InstructionFetchFinishedEventNotFiredException(
            /*instructionAddress:*/ (short)(_Registers.getPC() - executionContext.getOpcodeBytes().size()),
            /*fetchedBytes:*/ toByteArray(executionContext.getOpcodeBytes()), null, null);
    }

    private void CheckAutoStopForHaltOnDi()
    {
        if(AutoStopOnDiPlusHalt && executionContext.getIsHaltInstruction() && !getInterruptsEnabled())
            executionContext.setStopReason(StopReason.DiPlusHalt);
    }

    private void CheckForAutoStopForRetWithStackEmpty()
    {
        if(AutoStopOnRetWithStackEmpty && executionContext.getIsRetInstruction() && StackIsEmpty())
            executionContext.setStopReason(StopReason.RetWithStackEmpty);
    }

    private void CheckForLdSpInstruction()
    {
        if(executionContext.getIsLdSpInstruction())
            startOfStack = _Registers.getSP();
    }

    private boolean StackIsEmpty()
    {
        return executionContext.getSpAfterInstructionFetch() == startOfStack;
    }

    private boolean getInterruptsEnabled() {
        return _Registers.getIFF1().intValue() == 1;
    }

    void FireAfterInstructionExecutionEvent(int tStates)
    {
        AfterInstructionExecution().fireEvent(new AfterInstructionExecutionEventArgs(
            toByteArray(executionContext.getOpcodeBytes()),
            /*stopper:*/ this,
            /*localUserState:*/ executionContext.getLocalUserStateFromPreviousEvent(),
            /*tStates:*/ tStates));
    }

    void InstructionExecutor_InstructionFetchFinished(InstructionFetchFinishedEvent e)
    {
        if(executionContext.getFetchComplete())
            return;

        executionContext.setFetchComplete(true);

        executionContext.setIsRetInstruction(e.getIsRetInstruction());
        executionContext.setIsLdSpInstruction(e.getIsLdSpInstruction());
        executionContext.setIsHaltInstruction(e.getIsHaltInstruction());
        executionContext.setIsEiOrDiInstruction(e.getIsEiOrDiInstruction());

        executionContext.setSpAfterInstructionFetch(_Registers.getSP());

        var eventArgs = FireBeforeInstructionExecutionEvent();
        executionContext.setLocalUserStateFromPreviousEvent(eventArgs.getLocalUserState());
    }

    void FireBeforeInstructionFetchEvent()
    {
        var eventArgs = new BeforeInstructionFetchEventArgs(/*stopper:*/ this);

        if(BeforeInstructionFetch != null) {
            executionContext.setExecutingBeforeInstructionEvent(true);
            try {
                BeforeInstructionFetch().fireEvent(eventArgs);
            }
            finally {
                executionContext.setExecutingBeforeInstructionEvent(false);
            }
        }

        executionContext.setLocalUserStateFromPreviousEvent(eventArgs.getLocalUserState());
    }

    BeforeInstructionExecutionEventArgs FireBeforeInstructionExecutionEvent()
    {
        var eventArgs = new BeforeInstructionExecutionEventArgs(
            toByteArray(executionContext.getOpcodeBytes()),
            executionContext.getLocalUserStateFromPreviousEvent());

        if(BeforeInstructionExecution != null)
            BeforeInstructionExecution().fireEvent(eventArgs);

        return eventArgs;
    }

    public void Reset()
    {
        _Registers.setIFF1(Bit.of(0));
        _Registers.setIFF2(Bit.of(0));
        _Registers.setPC((short)0);
        _Registers.setAF((short)0xFFFF);
        _Registers.setSP((short)0xFFFF);
        _InterruptMode = 0;

        _nmiInterruptPending = false;
        isHalted = false;

        TStatesElapsedSinceReset = 0;
        startOfStack = _Registers.getSP();
    }

    public int ExecuteNextInstruction()
    {
        return InstructionExecutionLoop(/*isSingleInstruction:*/ true);
    }

//#endregion

//#region Information and state

    private long TStatesElapsedSinceStart; public long getTStatesElapsedSinceStart() { return TStatesElapsedSinceStart; }

    private long TStatesElapsedSinceReset; public long getTStatesElapsedSinceReset() { return TStatesElapsedSinceReset; }

    private konamiman.Enums.StopReason _StopReason; public StopReason getStopReason() { return _StopReason; }

    private ProcessorState State; public ProcessorState getState() { return State; }

    private Object UserState; public Object getUserState() { return UserState; } public void setUserState(Object value) { UserState = value; }

//    @Override
//    public boolean getIsHalted() {
//        return false;
//    }

    protected boolean isHalted;
    public boolean getIsHalted() { return isHalted; }

    private byte _InterruptMode;
    public byte getInterruptMode()
        {
            return _InterruptMode;
        }
    public void setInterruptMode(byte value) // TODO SetInterruptMode
        {
            if(value > 2)
                throw new IllegalArgumentException("Interrupt mode can be set to 0, 1 or 2 only");

            _InterruptMode = value;
        }

//    @Override
//    public short getStartOfStack() {
//        return 0;
//    }

    protected short startOfStack;
    public short getStartOfStack() { return startOfStack; }

//#endregion

//#region Inside and outside world

    private IZ80Registers _Registers;
    public IZ80Registers getRegisters() {
        return _Registers;
    }
    public void setRegisters(IZ80Registers value) {
        if(value == null)
            throw new NullPointerException("Registers");

        _Registers = value;
    }

    private IMemory _Memory;
    public IMemory getMemory()
    {
        return _Memory;
    }
    public void setMemory(IMemory value)
    {
        if(value == null)
            throw new NullPointerException("Memory");

        _Memory = value;
    }

    private MemoryAccessMode[] memoryAccessModes = new MemoryAccessMode[MemorySpaceSize];

    public void SetMemoryAccessMode(short startAddress, int length, MemoryAccessMode mode)
    {
         SetArrayContents(memoryAccessModes, startAddress, length, mode);
    }

    private <T> void SetArrayContents(T[] array, short startIndex, int length, T value)
    {
        if (length < 0)
            throw new IllegalArgumentException("length can't be negative");
        if (startIndex + length > array.length)
            throw new IllegalArgumentException("start + length go beyond " + (array.length - 1));

        var data = new Object[length];
        Arrays.fill(data, value);
        System.arraycopy(data, 0, array, startIndex, length);
    }

    public MemoryAccessMode GetMemoryAccessMode(short address)
    {
        return memoryAccessModes[address];
    }

    private IMemory _PortsSpace;
    public IMemory getPortsSpace() {
        return _PortsSpace;
    }
    public void setPortsSpace(IMemory value)
    {
        if(value == null)
            throw new NullPointerException("PortsSpace");

        _PortsSpace = value;
    }

    private MemoryAccessMode[] portsAccessModes = new MemoryAccessMode[PortSpaceSize];

    public void SetPortsSpaceAccessMode(byte startPort, int length, MemoryAccessMode mode)
    {
        SetArrayContents(portsAccessModes, startPort, length, mode);
    }

    public MemoryAccessMode GetPortAccessMode(byte portNumber)
    {
        return portsAccessModes[portNumber];
    }

    private List<IZ80InterruptSource> _InterruptSources;
    public List<IZ80InterruptSource> getInterruptSources() { return _InterruptSources; }
    public void setInterruptSources(List<IZ80InterruptSource> value) { _InterruptSources = value; }

    public void RegisterInterruptSource(IZ80InterruptSource source)
    {
        if(_InterruptSources.contains(source))
            return;

        _InterruptSources.add(source);
        source.NmiInterruptPulse().addListener(args -> _nmiInterruptPending = true);
    }

    private final Object nmiInterruptPendingSync = new Object();
    private boolean _nmiInterruptPending;
    private boolean getNmiInterruptPending() {
            synchronized (nmiInterruptPendingSync) {
                var value = _nmiInterruptPending;
                _nmiInterruptPending = false;
                return value;
            }
        }
    public void setNmiInterruptPending(boolean value)
        {
            synchronized (nmiInterruptPendingSync) {
                _nmiInterruptPending = value;
            }
        }

    public List<IZ80InterruptSource> GetRegisteredInterruptSources()
    {
        return _InterruptSources;
    }

    public void UnregisterAllInterruptSources()
    {
        for (var source : _InterruptSources) {
            source.NmiInterruptPulse().removeListener(args -> _nmiInterruptPending = true); // TODO
        }

        _InterruptSources.clear();
    }

//#endregion

//#region Configuration

    private float effectiveClockFrequency;

    private float _ClockFrequencyInMHz;
    public float getClockFrequencyInMHz()
        {
            return _ClockFrequencyInMHz;
        }
        public void setClockFrequencyInMHz(float value)
        {
            SetEffectiveClockFrequency(value, _ClockSpeedFactor);
            _ClockFrequencyInMHz = value;
        }

    private void SetEffectiveClockFrequency(float clockFrequency, float clockSpeedFactor)
    {
        float effectiveClockFrequency = clockFrequency * clockSpeedFactor;
        if((effectiveClockFrequency > MaxEffectiveClockSpeed) ||
            (effectiveClockFrequency < MinEffectiveClockSpeed))
            throw new IllegalArgumentException(String.format("Clock frequency multiplied by clock speed factor must be a number between %f and %d", MinEffectiveClockSpeed, MaxEffectiveClockSpeed));

        this.effectiveClockFrequency = effectiveClockFrequency;
        if(clockSynchronizer != null)
            clockSynchronizer.setEffectiveClockFrequencyInMHz(effectiveClockFrequency);
    }

    private float _ClockSpeedFactor;
    public float getClockSpeedFactor()
        {
            return _ClockSpeedFactor;
        }
        public void setClockSpeedFactor(float value)
        {
            SetEffectiveClockFrequency(_ClockFrequencyInMHz, value);
            _ClockSpeedFactor = value;
        }

    private boolean AutoStopOnDiPlusHalt; public boolean getAutoStopOnDiPlusHalt() { return AutoStopOnDiPlusHalt; } public void setAutoStopOnDiPlusHalt(boolean value) { AutoStopOnDiPlusHalt = value; }

    private boolean AutoStopOnRetWithStackEmpty; public boolean getAutoStopOnRetWithStackEmpty() { return AutoStopOnRetWithStackEmpty; } public void setAutoStopOnRetWithStackEmpty(boolean value) { AutoStopOnRetWithStackEmpty = value; }

    private byte[] memoryWaitStatesForM1 = new byte[MemorySpaceSize];

    public void SetMemoryWaitStatesForM1(short startAddress, int length, byte waitStates)
    {
//        SetArrayContents(memoryWaitStatesForM1, startAddress, length, waitStates);
        Arrays.fill(memoryWaitStatesForM1, startAddress, startAddress + length, waitStates);
    }

    public byte GetMemoryWaitStatesForM1(short address)
    {
        return memoryWaitStatesForM1[address];
    }

    private byte[] memoryWaitStatesForNonM1 = new byte[MemorySpaceSize];

    public void SetMemoryWaitStatesForNonM1(short startAddress, int length, byte waitStates)
    {
//        SetArrayContents(memoryWaitStatesForNonM1, startAddress, length, waitStates);
        Arrays.fill(memoryWaitStatesForNonM1, startAddress, startAddress + length, waitStates);
    }

    public byte GetMemoryWaitStatesForNonM1(short address)
    {
        return memoryWaitStatesForNonM1[address];
    }

    private byte[] portWaitStates = new byte[PortSpaceSize];

    public void SetPortWaitStates(short startPort, int length, byte waitStates)
    {
//        SetArrayContents(portWaitStates, startPort, length, waitStates);
        Arrays.fill(portWaitStates, startPort, startPort + length, waitStates);
    }

    public byte GetPortWaitStates(byte portNumber)
    {
        return portWaitStates[portNumber];
    }

    private IZ80InstructionExecutor _InstructionExecutor;
    public IZ80InstructionExecutor getInstructionExecutor() {
        return _InstructionExecutor;
    }
    public void setInstructionExecutor(IZ80InstructionExecutor value) {
        if(value == null)
            throw new NullPointerException("InstructionExecutor");

        if(_InstructionExecutor != null)
            _InstructionExecutor.InstructionFetchFinished().addListener(this::InstructionExecutor_InstructionFetchFinished);

        _InstructionExecutor = value;
        _InstructionExecutor.setProcessorAgent(this);
        _InstructionExecutor.InstructionFetchFinished().addListener(this::InstructionExecutor_InstructionFetchFinished);
    }

    private IClockSynchronizer clockSynchronizer;
    public IClockSynchronizer getClockSynchronizer() {
        return clockSynchronizer;
    }
    public void setClockSynchronizer(IClockSynchronizer value) {
        clockSynchronizer = value;
        if (value == null)
            return;

        clockSynchronizer.setEffectiveClockFrequencyInMHz(effectiveClockFrequency);
    }

    @Override
    public EventHandler<MemoryAccessEventArgs> MemoryAccess() {
        return MemoryAccess;
    }

    @Override
    public EventHandler<BeforeInstructionFetchEventArgs> BeforeInstructionFetch() {
        return BeforeInstructionFetch;
    }

    @Override
    public EventHandler<BeforeInstructionExecutionEventArgs> BeforeInstructionExecution() {
        return BeforeInstructionExecution;
    }

    @Override
    public EventHandler<AfterInstructionExecutionEventArgs> AfterInstructionExecution() {
        return AfterInstructionExecution;
    }

//#endregion

//#region Events

    public /* event */ EventHandler<MemoryAccessEventArgs> MemoryAccess = new EventHandler<>();

    public /* event */ EventHandler<BeforeInstructionFetchEventArgs> BeforeInstructionFetch = new EventHandler<>();

    public /* event */ EventHandler<BeforeInstructionExecutionEventArgs> BeforeInstructionExecution = new EventHandler<>();

    public /* event */ EventHandler<AfterInstructionExecutionEventArgs> AfterInstructionExecution = new EventHandler<>();

//#endregion

//#region Members of IZ80ProcessorAgent

    public byte fetchNextOpcode()
    {
        FailIfNoExecutionContext();

        if(executionContext.getFetchComplete())
            throw new IllegalArgumentException("FetchNextOpcode can be invoked only before the InstructionFetchFinished event has been raised.");

        byte opcode;
        if (executionContext.getPeekedOpcode() == null) {
            var address = _Registers.getPC();
            opcode = ReadFromMemoryOrPort(
                address,
                _Memory,
                GetMemoryAccessMode(address),
                MemoryAccessEventType.BeforeMemoryRead,
                MemoryAccessEventType.AfterMemoryRead,
                GetMemoryWaitStatesForM1(address));
        } else {
            executionContext.setAccummulatedMemoryWaitStates(executionContext.getAccummulatedMemoryWaitStates() +
                GetMemoryWaitStatesForM1(executionContext.getAddressOfPeekedOpcode()));
            opcode = executionContext.getPeekedOpcode();
            executionContext.setPeekedOpcode(null);
        }

        executionContext.getOpcodeBytes().add(opcode);
        _Registers.incPC();
        return opcode;
    }

    public byte PeekNextOpcode()
    {
        FailIfNoExecutionContext();

        if(executionContext.getFetchComplete())
            throw new IllegalArgumentException("PeekNextOpcode can be invoked only before the InstructionFetchFinished event has been raised.");

        if (executionContext.getPeekedOpcode() == null)
        {
            var address = _Registers.getPC();
            var opcode = ReadFromMemoryOrPort(
                address,
                _Memory,
                GetMemoryAccessMode(address),
                MemoryAccessEventType.BeforeMemoryRead,
                MemoryAccessEventType.AfterMemoryRead,
                /*waitStates:*/ (byte) 0);

            executionContext.setPeekedOpcode(opcode);
            executionContext.setAddressOfPeekedOpcode(_Registers.getPC());
            return opcode;
        }
        else
        {
            return executionContext.getPeekedOpcode();
        }
    }

    private void FailIfNoExecutionContext()
    {
        if(executionContext == null)
            throw new IllegalArgumentException("This method can be invoked only when an instruction is being executed.");
    }

    public byte readFromMemory(short address)
    {
        FailIfNoExecutionContext();
        FailIfNoInstructionFetchComplete();

        return ReadFromMemoryInternal(address);
    }

    private byte ReadFromMemoryInternal(short address)
    {
        return ReadFromMemoryOrPort(
            address,
            _Memory,
            GetMemoryAccessMode(address),
            MemoryAccessEventType.BeforeMemoryRead,
            MemoryAccessEventType.AfterMemoryRead,
            GetMemoryWaitStatesForNonM1(address));
    }

    protected void FailIfNoInstructionFetchComplete()
    {
        if(executionContext != null && !executionContext.getFetchComplete())
            throw new IllegalArgumentException("IZ80ProcessorAgent members other than FetchNextOpcode can be invoked only after the InstructionFetchFinished event has been raised.");
    }

    private byte ReadFromMemoryOrPort(
        short address,
        IMemory memory,
        MemoryAccessMode accessMode,
        MemoryAccessEventType beforeEventType,
        MemoryAccessEventType afterEventType,
        byte waitStates)
    {
        var beforeEventArgs = FireMemoryAccessEvent(beforeEventType, address, (byte) 0xFF, null, false);

        byte value;
        if(!beforeEventArgs.getCancelMemoryAccess() &&
            (accessMode == MemoryAccessMode.ReadAndWrite || accessMode == MemoryAccessMode.ReadOnly))
            value = memory.get(address);
        else
            value = beforeEventArgs.getValue();

        if(executionContext != null)
            executionContext.setAccummulatedMemoryWaitStates(executionContext.getAccummulatedMemoryWaitStates() + waitStates);

        var afterEventArgs = FireMemoryAccessEvent(
            afterEventType,
            address,
            value,
            beforeEventArgs.getLocalUserState(),
            beforeEventArgs.getCancelMemoryAccess());
        return afterEventArgs.getValue();
    }

    MemoryAccessEventArgs FireMemoryAccessEvent(
        MemoryAccessEventType eventType,
        short address,
        byte value,
        Object localUserState/*= null*/,
        boolean cancelMemoryAccess/*= false*/)
    {
        var eventArgs = new MemoryAccessEventArgs(eventType, address, value, localUserState, cancelMemoryAccess);
        MemoryAccess().fireEvent(eventArgs);
        return eventArgs;
    }

    public void WriteToMemory(short address, byte value)
    {
        FailIfNoExecutionContext();
        FailIfNoInstructionFetchComplete();

        WriteToMemoryInternal(address, value);
    }

    private void WriteToMemoryInternal(short address, byte value)
    {
        WritetoMemoryOrPort(
            address,
            value,
            _Memory,
            GetMemoryAccessMode(address),
            MemoryAccessEventType.BeforeMemoryWrite,
            MemoryAccessEventType.AfterMemoryWrite,
            GetMemoryWaitStatesForNonM1(address));
    }

    private void WritetoMemoryOrPort(
        short address,
        byte value,
        IMemory memory,
        MemoryAccessMode accessMode,
        MemoryAccessEventType beforeEventType,
        MemoryAccessEventType afterEventType,
        byte waitStates)
    {
        var beforeEventArgs = FireMemoryAccessEvent(beforeEventType, address, value, null, false);

        if(!beforeEventArgs.getCancelMemoryAccess() &&
            (accessMode == MemoryAccessMode.ReadAndWrite || accessMode == MemoryAccessMode.WriteOnly))
            memory.set(address, beforeEventArgs.getValue());

        if(executionContext != null)
            executionContext.setAccummulatedMemoryWaitStates(executionContext.getAccummulatedMemoryWaitStates() + waitStates);

        FireMemoryAccessEvent(
            afterEventType,
            address,
            beforeEventArgs.getValue(),
            beforeEventArgs.getLocalUserState(),
            beforeEventArgs.getCancelMemoryAccess());
    }

    public byte ReadFromPort(byte portNumber)
    {
        FailIfNoExecutionContext();
        FailIfNoInstructionFetchComplete();

        return ReadFromMemoryOrPort(
            portNumber,
            _PortsSpace,
            GetPortAccessMode(portNumber),
            MemoryAccessEventType.BeforePortRead,
            MemoryAccessEventType.AfterPortRead,
            GetPortWaitStates(portNumber));
    }

    public void WriteToPort(byte portNumber, byte value)
    {
        FailIfNoExecutionContext();
        FailIfNoInstructionFetchComplete();

        WritetoMemoryOrPort(
            portNumber,
            value,
            _PortsSpace,
            GetPortAccessMode(portNumber),
            MemoryAccessEventType.BeforePortWrite,
            MemoryAccessEventType.AfterPortWrite,
            GetPortWaitStates(portNumber));
    }

    public void SetInterruptMode(byte interruptMode)
    {
        FailIfNoExecutionContext();
        FailIfNoInstructionFetchComplete();

        this._InterruptMode = interruptMode;
    }

    public void Stop(boolean isPause/*= false*/)
    {
        FailIfNoExecutionContext();

        if(!executionContext.getExecutingBeforeInstructionEvent())
            FailIfNoInstructionFetchComplete();

        executionContext.setStopReason(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked);
    }

//    public IZ80Registers getRegisters() { /* IZ80ProcessorAgent */
//        return _Registers;
//    }

//#endregion

//#region Instruction execution context

    protected InstructionExecutionContext executionContext;

//#endregion

    public static byte[] toByteArray(List<Byte> o) {
        byte[] a = new byte[o.size()];
        IntStream.range(0, o.size()).forEach(i -> a[i] = o.get(i));
        return a;
    }
}

