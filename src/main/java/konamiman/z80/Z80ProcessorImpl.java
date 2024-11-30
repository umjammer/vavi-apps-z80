package konamiman.z80;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import konamiman.z80.enums.MemoryAccessEventType;
import konamiman.z80.enums.MemoryAccessMode;
import konamiman.z80.enums.ProcessorState;
import konamiman.z80.enums.StopReason;
import konamiman.z80.events.AfterInstructionExecutionEvent;
import konamiman.z80.events.BeforeInstructionExecutionEvent;
import konamiman.z80.events.BeforeInstructionFetchEvent;
import konamiman.z80.events.InstructionFetchFinishedEvent;
import konamiman.z80.events.MemoryAccessEvent;
import konamiman.z80.exceptions.InstructionFetchFinishedEventNotFiredException;
import konamiman.z80.impls.ClockSynchronizerImpl;
import konamiman.z80.impls.PlainMemory;
import konamiman.z80.impls.Z80RegistersImpl;
import konamiman.z80.instructions.core.Z80InstructionExecutorImpl;
import konamiman.z80.interfaces.ClockSynchronizer;
import konamiman.z80.interfaces.Memory;
import konamiman.z80.interfaces.Z80InstructionExecutor;
import konamiman.z80.interfaces.Z80InterruptSource;
import konamiman.z80.interfaces.Z80ProcessorAgent;
import konamiman.z80.interfaces.Z80Registers;
import konamiman.z80.utils.Bit;
import konamiman.z80.utils.InstructionExecutionContext;
import dotnet4j.util.compat.EventHandler;

import static java.lang.System.getLogger;
import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.toByteArray;


/**
 * The implementation of the {@link Z80Processor} interface.
 */
public class Z80ProcessorImpl implements Z80Processor, Z80ProcessorAgent {

    private static final Logger logger = getLogger(Z80ProcessorImpl.class.getName());

    private static final int MemorySpaceSize = 65536;
    private static final int PortSpaceSize = 256;

    private static final long MaxEffectiveClockSpeed = 100;
    private static final double MinEffectiveClockSpeed = 0.001;

    private static final short NmiServiceRoutine = 0x66;
    private static final byte NOP_opcode = 0x00;
    private static final byte RST38h_opcode = (byte) 0xFF;

    public Z80ProcessorImpl() {
        clockSynchronizer = new ClockSynchronizerImpl();

        clockFrequencyInMHz = 4;
        _ClockSpeedFactor = 1;

        autoStopOnDiPlusHalt = true;
        autoStopOnRetWithStackEmpty = false;
        startOfStack = (short) 0xFFFF;

        setMemoryWaitStatesForM1((short) 0, MemorySpaceSize, (byte) 0);
        setMemoryWaitStatesForNonM1((short) 0, MemorySpaceSize, (byte) 0);
        setPortWaitStates((short) 0, PortSpaceSize, (byte) 0);

        memory = new PlainMemory(MemorySpaceSize);
        portsSpace = new PlainMemory(PortSpaceSize);

        setMemoryAccessMode((short) 0, MemorySpaceSize, MemoryAccessMode.ReadAndWrite);
        setPortsSpaceAccessMode((byte) 0, PortSpaceSize, MemoryAccessMode.ReadAndWrite);

        registers = new Z80RegistersImpl();
        interruptSources = new ArrayList<>();

        setInstructionExecutor(new Z80InstructionExecutorImpl()); // must use setter

        stopReason = StopReason.NeverRan;
        state = ProcessorState.Stopped;
    }

//#region Processor control

    @Override
    public void start(Object userState/*= null*/) {
        if (userState != null)
            this.userState = userState;

        reset();
        tStatesElapsedSinceStart = 0;

        instructionExecutionLoop(false);
    }

    @Override
    public void continue_() {
        instructionExecutionLoop(false);
    }

    private int instructionExecutionLoop(boolean isSingleInstruction/*= false*/) {
        try {
            return instructionExecutionLoopCore(isSingleInstruction);
        } catch (Exception e) {
            state = ProcessorState.Stopped;
            stopReason = StopReason.ExceptionThrown;

            throw e;
        }
    }

    private int instructionExecutionLoopCore(boolean isSingleInstruction) {
        if (clockSynchronizer != null) clockSynchronizer.start();
        executionContext = new InstructionExecutionContext();
        stopReason = StopReason.NotApplicable;
        state = ProcessorState.Running;
        var totalTStates = 0;

        while (!executionContext.getMustStop()) {
            executionContext.startNewInstruction();

            fireBeforeInstructionFetchEvent();
            if (executionContext.getMustStop())
                break;

            var executionTStates = executeNextOpcode();

            totalTStates = executionTStates + executionContext.getAccumulatedMemoryWaitStates();
            tStatesElapsedSinceStart += totalTStates;
            tStatesElapsedSinceReset += totalTStates;

            throwIfNoFetchFinishedEventFired();

            if (!isSingleInstruction) {
                checkAutoStopForHaltOnDi();
                checkForAutoStopForRetWithStackEmpty();
                checkForLdSpInstruction();
            }

            fireAfterInstructionExecutionEvent(totalTStates);

            if (!isHalted)
                isHalted = executionContext.isHaltInstruction();

            var interruptTStates = acceptPendingInterrupt();
            totalTStates += interruptTStates;
            tStatesElapsedSinceStart += interruptTStates;
            tStatesElapsedSinceReset += interruptTStates;

            if (isSingleInstruction)
                executionContext.setStopReason(StopReason.ExecuteNextInstructionInvoked);
            else if (clockSynchronizer != null)
                clockSynchronizer.tryWait(totalTStates);
        }

        if (clockSynchronizer != null)
            clockSynchronizer.stop();
        this.stopReason = executionContext.getStopReason();
        this.state =
                stopReason == StopReason.PauseInvoked
                        ? ProcessorState.Paused
                        : ProcessorState.Stopped;

        executionContext = null;

        return totalTStates;
    }

    private int executeNextOpcode() {
        if (isHalted) {
            executionContext.getOpcodeBytes().add(NOP_opcode);
            return instructionExecutor.execute(NOP_opcode);
        }

        return instructionExecutor.execute(fetchNextOpcode());
    }

    private int acceptPendingInterrupt() {
        if (executionContext.isEiOrDiInstruction())
            return 0;

        if (isNmiInterruptPending()) {
            isHalted = false;
            registers.setIFF1(Bit.of(0));
            executeCall(NmiServiceRoutine);
            return 11;
        }

        if (!isInterruptsEnabled())
            return 0;

        var activeIntSource = interruptSources.stream().filter(Z80InterruptSource::isIntLineIsActive).findFirst().orElse(null);
        if (activeIntSource == null)
            return 0;

        registers.setIFF1(Bit.of(0));
        registers.setIFF2(Bit.of(0));
        isHalted = false;

        switch (interruptMode) {
        case 0:
            var opcode = activeIntSource.getValueOnDataBus().orElse((byte) 0xFF);
            instructionExecutor.execute(opcode);
            return 13;
        case 1:
            instructionExecutor.execute(RST38h_opcode);
            return 13;
        case 2:
            var pointerAddress = createShort(
                    /*lowByte:*/ activeIntSource.getValueOnDataBus().orElse((byte) 0xFF),
                    /*highByte:*/ registers.getI());
            var callAddress = createShort(
                    /*lowByte:*/ readFromMemoryInternal(pointerAddress),
                    /*highByte:*/ readFromMemoryInternal((short) (pointerAddress + 1)));
            executeCall(callAddress);
            return 19;
        }

        return 0;
    }

    @Override
    public void executeCall(short address) {
        var oldAddress = registers.getPC();
        var sp = (short) (registers.getSP() - 1);
        writeToMemoryInternal(sp, getHighByte(oldAddress));
        sp = (short) (sp - 1);
        writeToMemoryInternal(sp, getLowByte(oldAddress));

        registers.setSP(sp);
        registers.setPC(address);
    }

    @Override
    public void executeRet() {
        var sp = registers.getSP();
        var newPC = createShort(readFromMemoryInternal(sp), readFromMemoryInternal((short) (sp + 1)));

        registers.setPC(newPC);
        registers.addSP((short) 2);
    }

    private void throwIfNoFetchFinishedEventFired() throws InstructionFetchFinishedEventNotFiredException {
        if (executionContext.isFetchComplete())
            return;

        throw new InstructionFetchFinishedEventNotFiredException(
                /*instructionAddress:*/ (short) (registers.getPC() - executionContext.getOpcodeBytes().size()),
                /*fetchedBytes:*/ toByteArray(executionContext.getOpcodeBytes()), null, null);
    }

    private void checkAutoStopForHaltOnDi() {
        if (autoStopOnDiPlusHalt && executionContext.isHaltInstruction() && !isInterruptsEnabled())
            executionContext.setStopReason(StopReason.DiPlusHalt);
    }

    private void checkForAutoStopForRetWithStackEmpty() {
        if (autoStopOnRetWithStackEmpty && executionContext.isRetInstruction() && isStackEmpty())
            executionContext.setStopReason(StopReason.RetWithStackEmpty);
    }

    private void checkForLdSpInstruction() {
        if (executionContext.isLdSpInstruction())
            startOfStack = registers.getSP();
    }

    private boolean isStackEmpty() {
        return executionContext.getSpAfterInstructionFetch() == startOfStack;
    }

    private boolean isInterruptsEnabled() {
        return registers.getIFF1().intValue() == 1;
    }

    void fireAfterInstructionExecutionEvent(int tStates) {
        afterInstructionExecution.fireEvent(new AfterInstructionExecutionEvent(
                this, toByteArray(executionContext.getOpcodeBytes()),
                /*stopper:*/ this,
                /*localUserState:*/ executionContext.getLocalUserStateFromPreviousEvent(),
                /*tStates:*/ tStates));
    }

    void instructionExecutorInstructionFetchFinished(InstructionFetchFinishedEvent e) {
        if (executionContext.isFetchComplete())
            return;

        executionContext.setFetchComplete(true);

        executionContext.setRetInstruction(e.isRetInstruction());
        executionContext.setLdSpInstruction(e.isLdSpInstruction());
        executionContext.setHaltInstruction(e.isHaltInstruction());
        executionContext.setEiOrDiInstruction(e.isEiOrDiInstruction());

        executionContext.setSpAfterInstructionFetch(registers.getSP());

        var eventArgs = fireBeforeInstructionExecutionEvent();
        executionContext.setLocalUserStateFromPreviousEvent(eventArgs.getLocalUserState());
    }

    void fireBeforeInstructionFetchEvent() {
        var eventArgs = new BeforeInstructionFetchEvent(this,/*stopper:*/ this);

        if (beforeInstructionFetch != null) {
            executionContext.setExecutingBeforeInstructionEvent(true);
            try {
                beforeInstructionFetch.fireEvent(eventArgs);
            } finally {
                executionContext.setExecutingBeforeInstructionEvent(false);
            }
        }

        executionContext.setLocalUserStateFromPreviousEvent(eventArgs.getLocalUserState());
    }

    BeforeInstructionExecutionEvent fireBeforeInstructionExecutionEvent() {
        var eventArgs = new BeforeInstructionExecutionEvent(
                this, toByteArray(executionContext.getOpcodeBytes()),
                executionContext.getLocalUserStateFromPreviousEvent());

        if (beforeInstructionExecution != null)
            beforeInstructionExecution.fireEvent(eventArgs);

        return eventArgs;
    }

    @Override
    public void reset() {
        registers.setIFF1(Bit.of(0));
        registers.setIFF2(Bit.of(0));
        registers.setPC((short) 0);
        registers.setAF((short) 0xFFFF);
        registers.setSP((short) 0xFFFF);
        interruptMode = 0;

        nmiInterruptPending = false;
        isHalted = false;

        tStatesElapsedSinceReset = 0;
        startOfStack = registers.getSP();
    }

    @Override
    public int executeNextInstruction() {
        return instructionExecutionLoop(/*isSingleInstruction:*/ true);
    }

//#endregion

//#region Information and state

    private long tStatesElapsedSinceStart;
    @Override public long getTStatesElapsedSinceStart() { return tStatesElapsedSinceStart; }

    private long tStatesElapsedSinceReset;
    @Override public long getTStatesElapsedSinceReset() { return tStatesElapsedSinceReset; }

    private StopReason stopReason;
    @Override public StopReason getStopReason() { return stopReason; }

    private ProcessorState state;
    @Override public ProcessorState getState() { return state; }

    private Object userState;
    @Override public Object getUserState() { return userState; }
    @Override public void setUserState(Object value) { userState = value; }

//    @Override
//    public boolean getIsHalted() {
//        return false;
//    }

    protected boolean isHalted;
    @Override
    public boolean getIsHalted() { return isHalted; }

    private byte interruptMode;
    @Override
    public byte getInterruptMode()
        {
            return interruptMode;
        }
    @Override
    public void setInterruptMode(byte value) {
        if(value > 2)
            throw new IllegalArgumentException("Interrupt mode can be set to 0, 1 or 2 only");

        interruptMode = value;
    }

    protected short startOfStack;
    @Override
    public short getStartOfStack() { return startOfStack; }

//#endregion

//#region Inside and outside world

    private Z80Registers registers;
    @Override
    public Z80Registers getRegisters() {
        return registers;
    }
    @Override
    public void setRegisters(Z80Registers value) {
        if(value == null)
            throw new NullPointerException("Registers");

        registers = value;
    }

    private Memory memory;
    @Override
    public Memory getMemory() {
        return memory;
    }
    @Override
    public void setMemory(Memory value) {
        if(value == null)
            throw new NullPointerException("Memory");

        memory = value;
    }

    private MemoryAccessMode[] memoryAccessModes = new MemoryAccessMode[MemorySpaceSize];

    @Override
    public void setMemoryAccessMode(short startAddress, int length, MemoryAccessMode mode) {
        setArrayContents(memoryAccessModes, startAddress, length, mode);
    }

    private static <T> void setArrayContents(T[] array, short startIndex, int length, T value) {
        if (length < 0)
            throw new IllegalArgumentException("length can't be negative");
        if (startIndex + length > array.length)
            throw new IllegalArgumentException("start + length go beyond " + (array.length - 1));

        var data = new Object[length];
        Arrays.fill(data, value);
        System.arraycopy(data, 0, array, startIndex & 0xffff, length);
    }

    @Override
    public MemoryAccessMode getMemoryAccessMode(short address) {
        return memoryAccessModes[address & 0xffff];
    }

    private Memory portsSpace;
    @Override
    public Memory getPortsSpace() {
        return portsSpace;
    }
    /** @throws NullPointerException value is null */
    @Override
    public void setPortsSpace(Memory value) {
        if (value == null) throw new NullPointerException("PortsSpace");

        portsSpace = value;
    }

    private MemoryAccessMode[] portsAccessModes = new MemoryAccessMode[PortSpaceSize];

    @Override
    public void setPortsSpaceAccessMode(byte startPort, int length, MemoryAccessMode mode) {
        setArrayContents(portsAccessModes, (short) (startPort & 0xff), length, mode);
    }

    @Override
    public MemoryAccessMode getPortAccessMode(byte portNumber) {
        return portsAccessModes[portNumber & 0xff];
    }

    private List<Z80InterruptSource> interruptSources;
    @SuppressWarnings("rawtypes")
    private EventHandler.EventListener defaultListener = args -> setNmiInterruptPending(true);

    @Override
    @SuppressWarnings("unchecked")
    public void registerInterruptSource(Z80InterruptSource source) {
        if (interruptSources.contains(source)) {
logger.log(Level.DEBUG, "already contains source");
            return;
        }

        interruptSources.add(source);
        source.nmiInterruptPulse().addListener(defaultListener);
    }

    private final Object nmiInterruptPendingSync = new Object();
    /** don't refer directory */
    private boolean nmiInterruptPending;
    private boolean isNmiInterruptPending() {
        synchronized (nmiInterruptPendingSync) {
            var value = nmiInterruptPending;
            nmiInterruptPending = false;
            return value;
        }
    }
    public void setNmiInterruptPending(boolean value) {
        synchronized (nmiInterruptPendingSync) {
            nmiInterruptPending = value;
        }
    }

    @Override
    public List<Z80InterruptSource> getRegisteredInterruptSources() {
        return interruptSources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterAllInterruptSources() {
        for (var source : interruptSources) {
            source.nmiInterruptPulse().removeListener(defaultListener);
        }

        interruptSources.clear();
    }

//#endregion

//#region Configuration

    private float effectiveClockFrequency;

    private float clockFrequencyInMHz;
    @Override
    public float getClockFrequencyInMHz() {
        return clockFrequencyInMHz;
    }
    @Override
    public void setClockFrequencyInMHz(float value) {
        setEffectiveClockFrequency(value, _ClockSpeedFactor);
        clockFrequencyInMHz = value;
    }

    private void setEffectiveClockFrequency(float clockFrequency, float clockSpeedFactor) {
        float effectiveClockFrequency = clockFrequency * clockSpeedFactor;
        if ((effectiveClockFrequency > MaxEffectiveClockSpeed) ||
                (effectiveClockFrequency < MinEffectiveClockSpeed))
            throw new IllegalArgumentException(String.format("Clock frequency multiplied by clock speed factor must be a number between %f and %d", MinEffectiveClockSpeed, MaxEffectiveClockSpeed));

        this.effectiveClockFrequency = effectiveClockFrequency;
        if (clockSynchronizer != null)
            clockSynchronizer.setEffectiveClockFrequencyInMHz(effectiveClockFrequency);
    }

    private float _ClockSpeedFactor;
    @Override
    public float getClockSpeedFactor()
        {
            return _ClockSpeedFactor;
        }
    @Override
    public void setClockSpeedFactor(float value) {
        setEffectiveClockFrequency(clockFrequencyInMHz, value);
        _ClockSpeedFactor = value;
    }

    private boolean autoStopOnDiPlusHalt; @Override
    public boolean getAutoStopOnDiPlusHalt() { return autoStopOnDiPlusHalt; } @Override
    public void setAutoStopOnDiPlusHalt(boolean value) { autoStopOnDiPlusHalt = value; }

    private boolean autoStopOnRetWithStackEmpty; @Override
    public boolean getAutoStopOnRetWithStackEmpty() { return autoStopOnRetWithStackEmpty; } @Override
    public void setAutoStopOnRetWithStackEmpty(boolean value) { autoStopOnRetWithStackEmpty = value; }

    private byte[] memoryWaitStatesForM1 = new byte[MemorySpaceSize];

    @Override
    public void setMemoryWaitStatesForM1(short startAddress, int length, byte waitStates) {
//        SetArrayContents(memoryWaitStatesForM1, startAddress, length, waitStates);
        Arrays.fill(memoryWaitStatesForM1, startAddress & 0xffff, (startAddress & 0xffff) + length, waitStates);
    }

    @Override
    public byte getMemoryWaitStatesForM1(short address) {
        return memoryWaitStatesForM1[address & 0xffff];
    }

    private byte[] memoryWaitStatesForNonM1 = new byte[MemorySpaceSize];

    @Override
    public void setMemoryWaitStatesForNonM1(short startAddress, int length, byte waitStates) {
//        SetArrayContents(memoryWaitStatesForNonM1, startAddress, length, waitStates);
        Arrays.fill(memoryWaitStatesForNonM1, startAddress & 0xffff, (startAddress & 0xffff) + length, waitStates);
    }

    @Override
    public byte getMemoryWaitStatesForNonM1(short address) {
        return memoryWaitStatesForNonM1[address & 0xffff];
    }

    private byte[] portWaitStates = new byte[PortSpaceSize];

    @Override
    public void setPortWaitStates(short startPort, int length, byte waitStates) {
//        SetArrayContents(portWaitStates, startPort, length, waitStates);
        Arrays.fill(portWaitStates, startPort & 0xffff, (startPort & 0xffff) + length, waitStates);
    }

    @Override
    public byte getPortWaitStates(byte portNumber) {
        return portWaitStates[portNumber & 0xff];
    }

    /** don't set directory, use setter */
    private Z80InstructionExecutor instructionExecutor;
    @Override
    public Z80InstructionExecutor getInstructionExecutor() {
        return instructionExecutor;
    }
    @Override
    public void setInstructionExecutor(Z80InstructionExecutor value) {
        if(value == null)
            throw new NullPointerException("InstructionExecutor");

        if(instructionExecutor != null)
            instructionExecutor.instructionFetchFinished().addListener(this::instructionExecutorInstructionFetchFinished);

        instructionExecutor = value;
        instructionExecutor.setProcessorAgent(this);
        instructionExecutor.instructionFetchFinished().addListener(this::instructionExecutorInstructionFetchFinished);
    }

    private ClockSynchronizer clockSynchronizer;
    @Override
    public ClockSynchronizer getClockSynchronizer() {
        return clockSynchronizer;
    }
    @Override
    public void setClockSynchronizer(ClockSynchronizer value) {
        clockSynchronizer = value;
        if (value == null)
            return;

        clockSynchronizer.setEffectiveClockFrequencyInMHz(effectiveClockFrequency);
    }

    @Override
    public EventHandler<MemoryAccessEvent> memoryAccess() {
        return memoryAccess;
    }

    @Override
    public EventHandler<BeforeInstructionFetchEvent> beforeInstructionFetch() {
        return beforeInstructionFetch;
    }

    @Override
    public EventHandler<BeforeInstructionExecutionEvent> beforeInstructionExecution() {
        return beforeInstructionExecution;
    }

    @Override
    public EventHandler<AfterInstructionExecutionEvent> afterInstructionExecution() {
        return afterInstructionExecution;
    }

//#endregion

//#region Events

    public EventHandler<MemoryAccessEvent> memoryAccess = new EventHandler<>();

    public EventHandler<BeforeInstructionFetchEvent> beforeInstructionFetch = new EventHandler<>();

    public EventHandler<BeforeInstructionExecutionEvent> beforeInstructionExecution = new EventHandler<>();

    public EventHandler<AfterInstructionExecutionEvent> afterInstructionExecution = new EventHandler<>();

//#endregion

//#region Members of Z80ProcessorAgent

    @Override
    public byte fetchNextOpcode() {
        failIfNoExecutionContext();

        if (executionContext.isFetchComplete())
            throw new IllegalArgumentException("FetchNextOpcode can be invoked only before the InstructionFetchFinished event has been raised.");

        byte opcode;
        if (executionContext.getPeekedOpcode() == null) {
            var address = registers.getPC();
            opcode = readFromMemoryOrPort(
                    address,
                    memory,
                    getMemoryAccessMode(address),
                    MemoryAccessEventType.BeforeMemoryRead,
                    MemoryAccessEventType.AfterMemoryRead,
                    getMemoryWaitStatesForM1(address));
        } else {
            executionContext.setAccumulatedMemoryWaitStates(executionContext.getAccumulatedMemoryWaitStates() +
                    getMemoryWaitStatesForM1(executionContext.getAddressOfPeekedOpcode()));
            opcode = executionContext.getPeekedOpcode();
            executionContext.setPeekedOpcode(null);
        }

        executionContext.getOpcodeBytes().add(opcode);
        registers.incPC();
        return opcode;
    }

    @Override
    public byte peekNextOpcode() {
        failIfNoExecutionContext();

        if (executionContext.isFetchComplete())
            throw new IllegalArgumentException("PeekNextOpcode can be invoked only before the InstructionFetchFinished event has been raised.");

        if (executionContext.getPeekedOpcode() == null) {
            var address = registers.getPC();
            var opcode = readFromMemoryOrPort(
                    address,
                    memory,
                    getMemoryAccessMode(address),
                    MemoryAccessEventType.BeforeMemoryRead,
                    MemoryAccessEventType.AfterMemoryRead,
                    /*waitStates:*/ (byte) 0);

            executionContext.setPeekedOpcode(opcode);
            executionContext.setAddressOfPeekedOpcode(registers.getPC());
            return opcode;
        } else {
            return executionContext.getPeekedOpcode();
        }
    }

    private void failIfNoExecutionContext() {
        if (executionContext == null)
            throw new IllegalStateException("This method can be invoked only when an instruction is being executed.");
    }

    @Override
    public byte readFromMemory(short address) {
        failIfNoExecutionContext();
        failIfNoInstructionFetchComplete();

        return readFromMemoryInternal(address);
    }

    private byte readFromMemoryInternal(short address) {
        return readFromMemoryOrPort(
                address,
                memory,
                getMemoryAccessMode(address),
                MemoryAccessEventType.BeforeMemoryRead,
                MemoryAccessEventType.AfterMemoryRead,
                getMemoryWaitStatesForNonM1(address));
    }

    protected void failIfNoInstructionFetchComplete() {
        if (executionContext != null && !executionContext.isFetchComplete())
            throw new IllegalStateException("Z80ProcessorAgent members other than FetchNextOpcode can be invoked only after the InstructionFetchFinished event has been raised.");
    }

    private byte readFromMemoryOrPort(
            short address,
            Memory memory,
            MemoryAccessMode accessMode,
            MemoryAccessEventType beforeEventType,
            MemoryAccessEventType afterEventType,
            byte waitStates) {
        var beforeEventArgs = fireMemoryAccessEvent(beforeEventType, address, (byte) 0xFF, null, false);

        byte value;
        if (!beforeEventArgs.getCancelMemoryAccess() &&
                (accessMode == MemoryAccessMode.ReadAndWrite || accessMode == MemoryAccessMode.ReadOnly))
            value = memory.get(address & 0xffff);
        else
            value = beforeEventArgs.getValue();

        if (executionContext != null)
            executionContext.setAccumulatedMemoryWaitStates(executionContext.getAccumulatedMemoryWaitStates() + waitStates);

        var afterEventArgs = fireMemoryAccessEvent(
                afterEventType,
                address,
                value,
                beforeEventArgs.getLocalUserState(),
                beforeEventArgs.getCancelMemoryAccess());
        return afterEventArgs.getValue();
    }

    MemoryAccessEvent fireMemoryAccessEvent(
            MemoryAccessEventType eventType,
            short address,
            byte value,
            Object localUserState/*= null*/,
            boolean cancelMemoryAccess/*= false*/) {
        var eventArgs = new MemoryAccessEvent(this, eventType, address, value, localUserState, cancelMemoryAccess);
        memoryAccess.fireEvent(eventArgs);
        return eventArgs;
    }

    @Override
    public void writeToMemory(short address, byte value) {
        failIfNoExecutionContext();
        failIfNoInstructionFetchComplete();

        writeToMemoryInternal(address, value);
    }

    private void writeToMemoryInternal(short address, byte value) {
        writeToMemoryOrPort(
                address,
                value,
                memory,
                getMemoryAccessMode(address),
                MemoryAccessEventType.BeforeMemoryWrite,
                MemoryAccessEventType.AfterMemoryWrite,
                getMemoryWaitStatesForNonM1(address));
    }

    private void writeToMemoryOrPort(
            short address,
            byte value,
            Memory memory,
            MemoryAccessMode accessMode,
            MemoryAccessEventType beforeEventType,
            MemoryAccessEventType afterEventType,
            byte waitStates) {
        var beforeEventArgs = fireMemoryAccessEvent(beforeEventType, address, value, null, false);

        if (!beforeEventArgs.getCancelMemoryAccess() &&
                (accessMode == MemoryAccessMode.ReadAndWrite || accessMode == MemoryAccessMode.WriteOnly))
            memory.set(address & 0xffff, beforeEventArgs.getValue());

        if (executionContext != null)
            executionContext.setAccumulatedMemoryWaitStates(executionContext.getAccumulatedMemoryWaitStates() + waitStates);

        fireMemoryAccessEvent(
                afterEventType,
                address,
                beforeEventArgs.getValue(),
                beforeEventArgs.getLocalUserState(),
                beforeEventArgs.getCancelMemoryAccess());
    }

    @Override
    public byte readFromPort(byte portNumber) {
        failIfNoExecutionContext();
        failIfNoInstructionFetchComplete();

        return readFromMemoryOrPort(
                portNumber,
                portsSpace,
                getPortAccessMode(portNumber),
                MemoryAccessEventType.BeforePortRead,
                MemoryAccessEventType.AfterPortRead,
                getPortWaitStates(portNumber));
    }

    @Override
    public void writeToPort(byte portNumber, byte value) {
        failIfNoExecutionContext();
        failIfNoInstructionFetchComplete();

        writeToMemoryOrPort(
                portNumber,
                value,
                portsSpace,
                getPortAccessMode(portNumber),
                MemoryAccessEventType.BeforePortWrite,
                MemoryAccessEventType.AfterPortWrite,
                getPortWaitStates(portNumber));
    }

    @Override
    public void setInterruptMode2(byte interruptMode) {
        failIfNoExecutionContext();
        failIfNoInstructionFetchComplete();

        this.interruptMode = interruptMode;
    }

    @Override
    public void stop(boolean isPause/*= false*/) {
        failIfNoExecutionContext();

        if (!executionContext.isExecutingBeforeInstructionEvent())
            failIfNoInstructionFetchComplete();

        executionContext.setStopReason(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked);
    }

//    public Z80RegistersImpl getRegisters() { /* Z80ProcessorAgent */
//        return registers;
//    }

//#endregion

//#region Instruction execution context

    protected InstructionExecutionContext executionContext;

//#endregion

}

