package konamiman;

import konamiman.DependenciesInterfaces.IClockSynchronizer;
import konamiman.DependenciesInterfaces.IMemory;
import konamiman.DependenciesInterfaces.IZ80InstructionExecutor;
import konamiman.DependenciesInterfaces.IZ80InterruptSource;
import konamiman.DependenciesInterfaces.IZ80Registers;
import konamiman.Enums.MemoryAccessMode;
import konamiman.Enums.ProcessorState;
import konamiman.Enums.StopReason;
import konamiman.EventArgs.AfterInstructionExecutionEventArgs;
import konamiman.EventArgs.BeforeInstructionExecutionEventArgs;
import konamiman.EventArgs.BeforeInstructionFetchEventArgs;
import konamiman.EventArgs.MemoryAccessEventArgs;
import vavi.util.dotnet.EventHandler;


/**
 * Represents a Z80 processor class that can be used to develop processor simulators or computer emulators.
 *
 * <remarks>
 * <para>
 * The Z80 processor class is intended to be used in synchronous mode and controlled by events.
 * You simply configure the instance, subscribe to the {@link IZ80Processor.MemoryAccess},
 * {@link IZ80Processor.BeforeInstructionExecution} and {@link IZ80Processor.AfterInstructionExecution} events
 * and invoke the {@link IZ80Processor.Start} method.
 * The method returns when the processor stops execution for whatever reason (see {@link IZ80Processor.StopReason}).
 * </para>
 * <para>
 * The {@link IZ80Processor.MemoryAccess},
 * {@link IZ80Processor.BeforeInstructionExecution} and {@link IZ80Processor.AfterInstructionExecution} events
 * provide full control of the memory and ports access and the instructions executions. During these events
 * you can examine and alter the memory contents and even stop the processor execution.
 * Extra control on the memory and registers can be achieved by using custom implementations of
 * {@link IMemory} and {@link IZ80Registers}. The instruction execution engine itself can be
 * replaced as well by a custom implementation, see the {@link InstructionExecutor} property.
 * </para>
 * <para>
 * An alternative way of using the class is to use the {@link IZ80Processor.ExecuteNextInstruction} method.
 * This method will simply execute the next instruction (as pointed by the PC register, see {@link IZ80Processor.Registers})
 * and then returns immediately. This can be useful to allow for step-by-step debugging of Z80 code.
 * </para>
 * <para>
 * The default configuration when the class is instantiated is:
 * <list type="bullet">
 * <item><description>{@link IZ80Processor.ClockFrequencyInMHz} = 4</description></item>
 * <item><description>{@link IZ80Processor.ClockSpeedFactor} = 1</description></item>
 * <item><description>{@link IZ80Processor.AutoStopOnDiPlusHalt} = true</description></item>
 * <item><description>{@link IZ80Processor.AutoStopOnRetWithStackEmpty} = false</description></item>
 * <item><description>Memory and ports wait states: all zeros</description></item>
 * <item><description>{@link IZ80Processor.Memory} = an instance of {@link PlainMemory}</description></item>
 * <item><description>{@link IZ80Processor.PortsSpace} = an instance of {@link PlainMemory}</description></item>
 * <item><description>{@link IZ80Processor.Registers} = an instance of {@link Z80Registers}</description></item>
 * <item><description>Memory and ports access modes = all {@link MemoryAccessMode.ReadAndWrite}</description></item>
 * <item><description>{@link IZ80Processor.InstructionExecutor} = an instance of {@link Z80InstructionExecutor}</description></item>
 * <item><description>{@link ClockSynchronizer} = an instance of {@link ClockSynchronizer}</description></item>
 * </list>
 * </para>
 */
public interface IZ80Processor {

//#region Processor control

    /**
     * Performs a reset (see {@link IZ80Processor.Reset}) and sets the processor in running state.
     * This method cannot be invoked from an event handler.
     *
     * <remarks>
     * The method will finish when the {@link IExecutionStopper.Stop} method is invoked from within an
     * event handler, when a HALT instruction is executed with the interrupts disabled (only if
     * {@link IZ80Processor.AutoStopOnDiPlusHalt} is <b>true</b>), or when a RET instruction is executed
     * and the stack is empty (only if {@link IZ80Processor.AutoStopOnRetWithStackEmpty} is <b>true</b>).
     *
     * @param userState If this value is not null, it will be copied to the
     * {@link IZ80Processor.UserState} property.
     */
    void Start(Object userState/*= null*/);

    /**
     * Sets the processor in running state without first doing a reset, thus preserving the state of all the registers.
     * This method cannot be invoked from an event handler.
     *
     * @throws InvalidOperationException">The method is invoked from within an event handler.
     */
    void Continue();

    /**
     * Resets all registers to its initial state. The running state of the processor is not modified.
     *
     * <para>
     * This method sets the PC, IFF1, and IFF2 registers to 0, AF and SP to FFFFh,
     * and the interrupt mode to 0.
     * </para>
     * <para>
     * If the method is executed from a {@link IZ80Processor.MemoryAccess} event, the reset
     * will be effective after the current instruction execution finishes.
     */
    void Reset();

    /**
     * Executes the next instruction as pointed by the PC register, and then returns.
     * This method cannot be invoked from an event handler.
     *
     * <remarks>
     * <para>
     * During the execution of this method, the {@link IZ80Processor.MemoryAccess},
     * {@link IZ80Processor.BeforeInstructionExecution} and {@link IZ80Processor.AfterInstructionExecution}
     * events will be triggered as usual.
     * Altough not necessary, it is possible to invoke the {@link IExecutionStopper.Stop} method
     * during the {@link AfterInstructionExecutionEventArgs} event,
     * thus modifying the value of {@link IZ80Processor.StopReason}.
     * </para>
     * <para>
     * This method will never issue a reset. A manual call to {@link IZ80Processor.Reset} is needed
     * before the first invocation of this method if {@link IZ80Processor.Start} has never been invoked,
     * unless register state is set manually.
     * </para>
     *
     * @return The total amount of T states required to execute the instruction,
     * inclusing extra wait states.
     *
     * @throws InvalidOperationException">The method is invoked from within an event handler.
     */
    int ExecuteNextInstruction();

//#endregion

//#region Information and state

    /**
     * Obtains the count of T states elapsed since the processor execution started.
     *
     * <remarks>
     * <para>
     * This property is set to zero when the processor object is created, and when the
     * {@link IZ80Processor.Start} method is invoked. It is not affected by the
     * {@link IZ80Processor.Continue} and {@link IZ80Processor.Reset} methods.
     * </para>
     * <para>The value is updated after each relevant operation (memory access or instruction execution),
     * both in running mode and in single instruction execution mode.</para>
     */
    long getTStatesElapsedSinceStart();

    /**
     * Obtains the count of T states elapsed since the last reset.
     *
     * <remarks>
     * <para>
     * This property is set to zero when the processor object is created, and when the
     * {@link IZ80Processor.Start} method or the {@link IZ80Processor.Reset} are invoked.
     * It is not affected by the {@link IZ80Processor.Continue} method.
     * </para>
     * <para>The value is updated after each relevant operation (memory access or instruction execution),
     * both in running mode and in single instruction execution mode.</para>
     */
    long getTStatesElapsedSinceReset();

    /**
     * Obtains the reason for the processor not being in the running state,
     * that is, what triggered the last stop.
     */
    StopReason getStopReason();

    /**
     * Obtains the current processor execution state.
     */
    ProcessorState getState();

    /**
     * Contains an user-defined state object. This property exists for the client code convenience
     * and can be set to any value, the class code will never access it.
     */
    Object getUserState(); void setUserState(Object value);

    /**
     * Returns true when a HALT instruction is executed, returns to false when an interrupt request arrives.
     */
    boolean getIsHalted();

    /**
     * The current interrupt mode. It has always the value 0, 1 or 2.
     *
     * @throws ArgumentException">Attempt to set a value other than 0, 1 or 2
     */
    byte getInterruptMode(); void setInterruptMode(byte value);

    /**
     * The current address where the stack starts. If {@link AutoStopOnDiPlusHalt} is <b>true</b>,
     * execution will stop automatically when a <c>RET</c> instruction is executed with the SP register
     * having this value.
     *
     * <remarks>
     * This property is set to 0xFFFF when the class is first instantiated and when the {@link Start}
     * and {@link Reset} methods are executed. Also, when a <c>LD SP,x</c> instruction is executed,
     * this property is set to the new value of SP.
     */
    short getStartOfStack();

//#endregion

//#region Inside and outside world

    /**
     * Gets or sets the register set used by the processor.
     */
    IZ80Registers getRegisters(); void setRegisters(IZ80Registers value);

    /**
     * Gets or sets the visible memory for the processor.
     */
    IMemory getMemory(); void setMemory(IMemory value);

    /**
     * Sets the mode of a portion of the visible memory.
     *
     * @param startAddress First memory address that will be set
     * @param length Length of the memory portion that will be set
     * @param mode New memory mode
     * @throws ArgumentException"><c>startAddress</c> is less than 0,
     * or <c>startAddress</c> + <c>length</c> goes beyond 65535.
     */
    void SetMemoryAccessMode(short startAddress, int length, MemoryAccessMode mode);

    /**
     * Gets the access mode of a memory address.
     *
     * @param address The address to check
     * @return The current memory access mode for the address
     *
     * @throws ArgumentException"><c>address</c> is greater than 65536
     */
    MemoryAccessMode GetMemoryAccessMode(short address);

    /**
     * Gets or sets the visible ports space for the processor.
     */
    IMemory getPortsSpace(); void setPortsSpace(IMemory value);

    /**
     * Sets the access mode of a portion of the visible ports space.
     *
     * @param startPort First port that will be set
     * @param length Length of the mports space that will be set
     * @param mode New memory mode
     * @throws ArgumentException"><c>startAddress</c> is less than 0,
     * or <c>startAddress</c> + <c>length</c> goes beyond 255.
     */
    void SetPortsSpaceAccessMode(byte startPort, int length, MemoryAccessMode mode);

    /**
     * Gets the access mode of a port.
     *
     * @param portNumber The port number to check
     * @return The current memory access mode for the port
     *
     * @throws ArgumentException"><c>portNumber</c> is greater than 255.
     */
    MemoryAccessMode GetPortAccessMode(byte portNumber);

    /**
     * Registers a new interrupt source.
     *
     * @param source Interrupt source to register
     * <remarks>
     * After each instruction execution the processor will check if there is a non-maskable interrupt
     * or a maskable interrupt (in that order) pending  from any of the registered sources,
     * and process it as appropriate.
     */
    void RegisterInterruptSource(IZ80InterruptSource source);

    /**
     * Retrieves a read-only collection of all the registered interrupt sources.
     *
     * @return 
     */
    Iterable<IZ80InterruptSource> GetRegisteredInterruptSources();

    /**
     * Unregisters all the interrupt sources.
     *
     * <remarks>
     * After this method is executed there is no way for the processor to receive an interrupt request,
     * unless the {@link IZ80Processor.RegisterInterruptSource} method is invoked again.
     */
    void UnregisterAllInterruptSources();

//#endregion

//#region Configuration

    /**
     * Gets or sets the clock frequency in MegaHertzs.
     * This value cannot be changed while the processor is running or in single instruction execution mode.
     *
     * @throws ArgumentException">The product of {@link IZ80Processor.ClockSpeedFactor}
     * by the new value gives a number that is smaller than 0.001 or greater than 100.
     * @throws InvalidOperationException">The procesor is running or in single instruction execution mode.
     */
    float getClockFrequencyInMHz(); void setClockFrequencyInMHz(float value);

    /**
     * Gets or sets a value that is multiplied by the clock frequency to obtain the effective
     * clock frequency simulated by the processor.
     *
     * @throws ArgumentException">The product of {@link IZ80Processor.ClockFrequencyInMHz}
     * by the new value gives a number that is smaller than 0.001 or greater than 100.
     */
    float getClockSpeedFactor(); void setClockSpeedFactor(float value);

    /**
     * Gets or sets a value that indicates whether the processor should stop automatically or not when a HALT
     * instruction is executed with interrupts disabled.
     */
    boolean getAutoStopOnDiPlusHalt(); void setAutoStopOnDiPlusHalt(boolean value);

    /**
     * Gets or sets a value that indicates whether the processor should stop automatically when a RET
     * instruction is executed and the stack is empty.
     *
     * <remarks>
     * <para>"The stack is empty" means that the SP register has the value
     * it had when the {@link IZ80Processor.Start} method
     * was executed, or the value set by the last execution of a <c>LD SP,xx</c> instruction.
     * </para>
     * <para>
     * This setting is useful for testing simple programs, so that the processor stops automatically
     * as soon as the program finishes with a RET.
     * </para>
     */
    boolean getAutoStopOnRetWithStackEmpty(); void setAutoStopOnRetWithStackEmpty(boolean value);

    /**
     * Sets the wait states that will be simulated when accessing the visible memory
     * during the M1 cycle for a given address range.
     *
     * @param startAddress First memory address that will be configured
     * @param length Length of the memory portion that will be configured
     * @param waitStates New wait states
     * @throws InvalidOperationException"><c>startAddress</c> + <c>length</c> goes beyond 65535.
     */
    void SetMemoryWaitStatesForM1(short startAddress, int length, byte waitStates);

    /**
     * Obtains the wait states that will be simulated when accessing the visible memory
     * during the M1 cycle for a given address.
     *
     * @param address Address to het the wait states for
     * @return Current wait states during M1 for the specified address
     */
    byte GetMemoryWaitStatesForM1(short address);

    /**
     * Sets the wait states that will be simulated when accessing the visible memory
     * outside the M1 cycle for a given address range.
     *
     * @param startAddress First memory address that will be configured
     * @param length Length of the memory portion that will be configured
     * @param waitStates New wait states
     * @throws InvalidOperationException"><c>startAddress</c> + <c>length</c> goes beyond 65535.
     */
    void SetMemoryWaitStatesForNonM1(short startAddress, int length, byte waitStates);

    /**
     * Obtains the wait states that will be simulated when accessing the visible memory
     * outside the M1 cycle for a given address.
     *
     * @param address Address to het the wait states for
     * @return Current wait states outside M1 for the specified address
     */
    byte GetMemoryWaitStatesForNonM1(short address);

    /**
     * Sets the wait states that will be simulated when accessing the I/O ports.
     *
     * @param startPort First port that will be configured
     * @param length Length of the port range that will be configured
     * @param waitStates New wait states
     * @throws InvalidOperationException"><c>startAddress</c> + <c>length</c> goes beyond 255.
     */
    void SetPortWaitStates(short startPort, int length, byte waitStates);

    /**
     * Obtains the wait states that will be simulated when accessing the I/O ports
     * for a given port.
     *
     * @param portNumber Port number to het the wait states for
     * @return Current wait states for the specified port
     */
    byte GetPortWaitStates(byte portNumber);

    /**
     * Gets or set the instruction executor.
     */
    IZ80InstructionExecutor getInstructionExecutor(); void setInstructionExecutor(IZ80InstructionExecutor value);

    /**
     * Gets or sets the period waiter used to achieve proper timing on the execution flow.
     *
     * <remarks>
     *
     * This property can be set to _null_, in this case no clock syncrhonization will be performed
     * and the simulation will run at the maximum speed that the host system can provide.
     */
    IClockSynchronizer getClockSynchronizer(); void setClockSynchronizer(IClockSynchronizer value);

//#endregion

//#region Events

    /**
     * Memory access event. Is is triggered before and after each memory and port read and write.
     */
    /* event */ EventHandler<MemoryAccessEventArgs> MemoryAccess();

    /**
     * Pre-instruction fetch event. It is triggered before the next instruction is fetched.
     */
    /* event */ EventHandler<BeforeInstructionFetchEventArgs> BeforeInstructionFetch();

    /**
     * Pre-instruction execution event. It is triggered before an instruction is executed.
     */
    /* event */ EventHandler<BeforeInstructionExecutionEventArgs> BeforeInstructionExecution();

    /**
     * Post-instruction execution event. It is triggered after an instruction is executed.
     */
    /* event */ EventHandler<AfterInstructionExecutionEventArgs> AfterInstructionExecution();

//#endregion

//#region Utils

    /**
     * Simulate the execution of a CALL instruction by pushing the current content of the PC register
     * into the stack and setting it to the specified value.
     *
     * @param address New value for the PC register
     */
    void ExecuteCall(short address);

    /**
     * Simulate the execution of a RET instruction by setting the value of the PC register
     * from the value popped from the stack.
     */
    void ExecuteRet();

//#endregion
}
