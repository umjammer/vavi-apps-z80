package konamiman.z80;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import com.flextrade.jfixture.JFixture;
import dotnet4j.util.compat.EventHandler;
import konamiman.z80.enums.ProcessorState;
import konamiman.z80.enums.StopReason;
import konamiman.z80.events.InstructionFetchFinishedEvent;
import konamiman.z80.exceptions.InstructionFetchFinishedEventNotFiredException;
import konamiman.z80.interfaces.ClockSynchronizer;
import konamiman.z80.interfaces.Z80InstructionExecutor;
import konamiman.z80.interfaces.Z80ProcessorAgent;
import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.toByteArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class Z80ProcessorTests_InstructionExecution {

    private static final byte RET_opcode = (byte) 0xC9;
    private static final byte DI_opcode = (byte) 0xF3;
    private static final byte HALT_opcode = 0x76;
    private static final byte NOP_opcode = 0x00;
    private static final byte LD_SP_HL_opcode = (byte) 0xF9;

    Z80ProcessorForTests sut;
    JFixture fixture;
    ClockSynchronizer clockSyncHelper;

    @BeforeEach
    public void setup() {
        fixture = new JFixture();

        sut = new Z80ProcessorForTests();
        sut.setAutoStopOnRetWithStackEmpty(true);
        sut.getMemory().set(0, RET_opcode);
        sut.setMustFailIfNoInstructionFetchComplete(true);

        clockSyncHelper = mock(ClockSynchronizer.class);

        sut.setInstructionExecutor(new FakeInstructionExecutor() {{
            setProcessorAgent(sut);
        }});
        sut.setClockSynchronizer(clockSyncHelper);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(sut);
    }

//#region Start, Stop, Pause, Continue

    @Test
    public void Start_does_a_Reset() {
        sut.getRegisters().setPC(fixture.create(Short.TYPE));

        sut.start(null);

        assertEquals(1, sut.getRegisters().getPC());
    }

    void doBeforeFetch(Consumer<Byte> code) {
        ((FakeInstructionExecutor) sut.getInstructionExecutor()).extraBeforeFetchCode = code;
    }

    @Test
    public void Start_sets_StartOfStack_to_0xFFFF() {
        sut.setStartOfStack(fixture.create(Short.TYPE));

        sut.setAutoStopOnDiPlusHalt(true);
        sut.getMemory().set(0, DI_opcode);
        sut.getMemory().set(1, HALT_opcode);

        sut.start(null);

        assertEquals((short) 0xFFFF, sut.getStartOfStack());
    }

    @Test
    public void Starts_sets_global_state_if_passed_as_not_null() {
        var state = fixture.create(Object.class);
        sut.setUserState(null);

        sut.start(state);

        assertSame(state, sut.getUserState());
    }

    @Test
    public void Starts_does_not_set_global_state_if_passed_as_null() {
        sut.setUserState(fixture.create(Object.class));

        sut.start(null);

        assertNotNull(sut.getUserState());
    }

    @Test
    public void Continue_sets_execution_context_and_does_not_reset() {
        var pc = fixture.create(Short.TYPE);
        sut.getRegisters().setPC(pc);
        sut.getMemory().set(pc & 0xffff, RET_opcode);
        sut.setStartOfStack(sut.getRegisters().getSP());

        sut.continue_();

        assertEquals(inc(pc), sut.getRegisters().getPC());
    }

    @Test
    public void Start_sets_ProcessorState_to_running() {
        doBeforeFetch(b -> assertEquals(ProcessorState.Running, sut.getState()));

        sut.start(null);
    }

    private void AssertExecuted(byte opcode, int times) {
        var dictionary = ((FakeInstructionExecutor) sut.getInstructionExecutor()).timesEachInstructionIsExecuted;
        if (times == 0)
            assertFalse(dictionary.containsKey(opcode));
        else
            assertEquals(dictionary.get(opcode), times);
    }

    @Test
    public void StopRequest_stops_execution() {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, DI_opcode);
        sut.getMemory().set(2, RET_opcode);

        doAfterFetch(b -> {
            if (b == DI_opcode) sut.stop(false);
        });

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(StopReason.StopInvoked, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
    }

    @Test
    public void PauseRequest_stops_execution() {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, DI_opcode);
        sut.getMemory().set(2, RET_opcode);

        doAfterFetch(b -> {
            if (b == DI_opcode) sut.stop(/*isPause:*/ true);
        });

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(StopReason.PauseInvoked, sut.getStopReason());
        assertEquals(ProcessorState.Paused, sut.getState());
    }

    @Test
    public void Cannot_stop_if_no_execution_context() {
        assertThrows(IllegalStateException.class, () -> sut.stop(false));
    }

    @Test
    public void StopReason_is_not_applicable_while_executing() {
        doBeforeFetch(b -> assertEquals(StopReason.NotApplicable, sut.getStopReason()));

        sut.start(null);
    }

    @Test
    public void Has_proper_state_after_unhandled_exception() {
        doBeforeFetch(b -> {
            throw new RuntimeException();
        });

        assertThrows(RuntimeException.class, () -> sut.start(null));

        assertEquals(ProcessorState.Stopped, sut.getState());
        assertEquals(StopReason.ExceptionThrown, sut.getStopReason());
    }

//#endregion

//#region Conditions at runtime

    @Test
    public void Execution_invokes_InstructionExecutor_for_each_fetched_opcode() {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, DI_opcode);
        sut.getMemory().set(2, RET_opcode);

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 1);
    }

    @Test
    public void Cannot_change_interrupt_mode_from_agent_interface_if_no_execution_context() {
        assertThrows(IllegalStateException.class, () -> sut.setInterruptMode2((byte) 0));
    }

    @Test
    public void Can_change_interrupt_mode() {
        sut.setInterruptMode((byte) 0);

        doAfterFetch(b -> sut.setInterruptMode2((byte) 2));

        sut.start(null);

        assertEquals(2, sut.getInterruptMode());
    }

    void doAfterFetch(Consumer<Byte> code) {
        ((FakeInstructionExecutor) sut.getInstructionExecutor()).extraAfterFetchCode = code;
    }

//#endregion

//#region Auto-stop conditions

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Auto_stops_when_HALT_on_DI_found_or_when_RET_with_initial_stack_is_found_if_configured_to_do_so(boolean autoStopOnDiPlusHalt) {
        sut.setAutoStopOnDiPlusHalt(autoStopOnDiPlusHalt);
        sut.setAutoStopOnRetWithStackEmpty(!autoStopOnDiPlusHalt);

        sut.getMemory().set(0, DI_opcode);
        sut.getMemory().set(1, autoStopOnDiPlusHalt ? HALT_opcode : NOP_opcode);
        sut.getMemory().set(2, RET_opcode);

        doBeforeFetch(b -> sut.getRegisters().setIFF1(Bit.OFF));

        sut.start(null);

        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, autoStopOnDiPlusHalt ? 1 : 0);
        AssertExecuted(RET_opcode, autoStopOnDiPlusHalt ? 0 : 1);

        assertEquals(autoStopOnDiPlusHalt ? StopReason.DiPlusHalt : StopReason.RetWithStackEmpty, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
    }

    @Test
    public void Does_not_auto_stop_when_HALT_on_EI_found_regardless_of_AutoStopOnDiPlusHalt_is_true() {
        sut.setAutoStopOnDiPlusHalt(true);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.getMemory().set(0, DI_opcode);
        sut.getMemory().set(1, HALT_opcode);
        sut.getMemory().set(2, RET_opcode);

        doBeforeFetch(b -> sut.getRegisters().setIFF1(Bit.ON));

        var instructionsExecutedCount = new AtomicInteger(0);

        sut.afterInstructionExecution().addListener(args -> {
            if (instructionsExecutedCount.get() == 5)
                args.getExecutionStopper().stop(false);
            else
                instructionsExecutedCount.getAndIncrement();
        });

        sut.start(null);

        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(5, instructionsExecutedCount.get());
        assertEquals(StopReason.StopInvoked, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
    }

    @Test
    public void Auto_stops_when_RET_is_found_with_stack_equal_to_initial_value_if_AutoStopOnRetWithStackEmpty_is_true() {
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.getMemory().set(0, LD_SP_HL_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(2, DI_opcode);

        var spValue = fixture.create(Short.TYPE);

        doBeforeFetch(b -> sut.getRegisters().setIFF1(Bit.ON));
        doAfterFetch(b -> {
            if (b == LD_SP_HL_opcode) sut.getRegisters().setSP(spValue);
        });

        sut.start(null);

        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);
        AssertExecuted(DI_opcode, 0);

        assertEquals(StopReason.RetWithStackEmpty, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
    }

    @Test
    public void LD_SP_instructions_change_value_of_StartOfStack() {
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.getMemory().set(0, LD_SP_HL_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.setStartOfStack(fixture.create(Short.TYPE));

        var spValue = fixture.create(Short.TYPE);

        doAfterFetch(b -> {
            if (b == LD_SP_HL_opcode) sut.getRegisters().setSP(spValue);
        });

        sut.start(null);

        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);

        assertEquals(StopReason.RetWithStackEmpty, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
        assertEquals(spValue, sut.getStartOfStack());
    }

    @Test
    public void Does_not_auto_stops_when_RET_is_found_with_stack_not_equal_to_initial_value_if_AutoStopOnRetWithStackEmpty_is_true() {
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(2, RET_opcode);
        sut.getMemory().set(3, DI_opcode);

        var spValue = fixture.create(Short.TYPE);

        doBeforeFetch(b -> sut.getRegisters().setIFF1(Bit.ON));

        doAfterFetch(b -> {
            if (b == NOP_opcode)
                sut.getRegisters().addSP((short) 2);
            else if (b == RET_opcode)
                sut.getRegisters().subSP((short) 2);
        });

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(RET_opcode, 2);
        AssertExecuted(DI_opcode, 0);

        assertEquals(StopReason.RetWithStackEmpty, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
    }

//#endregion

//#region Before and after instruction execution events

    @Test
    public void Fires_before_and_after_instruction_execution_with_proper_opcodes_and_local_state() {
        var executeInvoked = new AtomicBoolean(false);
        var beforeFetchEventRaised = new AtomicBoolean(false);
        var beforeExecutionEventRaised = new AtomicBoolean(false);
        var afterEventRaised = new AtomicBoolean(false);
        var localState = fixture.create(Object.class);

        var instructionBytes = new byte[] {
                RET_opcode, HALT_opcode, DI_opcode, NOP_opcode
        };
        sut.getMemory().setContents(0, instructionBytes, 0, null);

        doBeforeFetch(b -> {
            sut.fetchNextOpcode();
            sut.fetchNextOpcode();
            sut.fetchNextOpcode();
        });

        sut.beforeInstructionFetch().addListener(e -> {
            beforeFetchEventRaised.set(true);
            assertFalse(executeInvoked.get());
            assertFalse(beforeExecutionEventRaised.get());
            assertFalse(afterEventRaised.get());
            executeInvoked.set(false);
            assertNull(e.getLocalUserState());

            e.setLocalUserState(localState);
        });

        sut.beforeInstructionExecution().addListener(e -> {
            beforeExecutionEventRaised.set(true);
            assertFalse(executeInvoked.get());
            assertTrue(beforeExecutionEventRaised.get());
            assertFalse(afterEventRaised.get());
            executeInvoked.set(true);
            assertArrayEquals(instructionBytes, e.getOpcode());
            assertEquals(localState, e.getLocalUserState());
        });

        sut.afterInstructionExecution().addListener(e -> {
            afterEventRaised.set(true);
            assertTrue(executeInvoked.get());
            assertTrue(beforeFetchEventRaised.get());
            assertTrue(beforeExecutionEventRaised.get());
            assertArrayEquals(instructionBytes, e.getOpcode());
            assertEquals(localState, e.getLocalUserState());
        });

        sut.start(null);

        assertTrue(beforeFetchEventRaised.get());
        assertTrue(beforeExecutionEventRaised.get());
        assertTrue(afterEventRaised.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Stops_execution_if_requested_from_AfterInstructionExecutionEvent(boolean isPause) {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, DI_opcode);
        sut.getMemory().set(2, HALT_opcode);
        sut.getMemory().set(3, RET_opcode);

        sut.afterInstructionExecution().addListener(e -> {
            if (e.getOpcode()[0] == DI_opcode)
                e.getExecutionStopper().stop(isPause);
        });

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 0);
        AssertExecuted(RET_opcode, 0);

        assertEquals(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked, sut.getStopReason());
        assertEquals(isPause ? ProcessorState.Paused : ProcessorState.Stopped, sut.getState());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Stops_execution_if_requested_from_BeforeInstructionFetchEvent(boolean isPause) {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, DI_opcode);
        sut.getMemory().set(2, HALT_opcode);
        sut.getMemory().set(3, RET_opcode);

        sut.beforeInstructionFetch().addListener(e -> {
            if (sut.getRegisters().getPC() == 2)
                e.getExecutionStopper().stop(isPause);
        });

        sut.start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 0);
        AssertExecuted(RET_opcode, 0);

        assertEquals(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked, sut.getStopReason());
        assertEquals(isPause ? ProcessorState.Paused : ProcessorState.Stopped, sut.getState());
    }

//#endregion

//#region Invoking agent members at the right time

    @Test
    public void ProcessorAgent_members_other_than_FetchNextOpcode_and_Registers_can_be_invoked_only_after_instruction_fetch_complete() {
        var address = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);

        sut.setMustFailIfNoInstructionFetchComplete(true);

        doBeforeFetch(b -> {
            assertThrows(IllegalStateException.class, () -> sut.readFromMemory(address));
            assertThrows(IllegalStateException.class, () -> sut.readFromPort(address));
            assertThrows(IllegalStateException.class, () -> sut.writeToMemory(address, value));
            assertThrows(IllegalStateException.class, () -> sut.writeToPort(address, value));
            assertThrows(IllegalStateException.class, () -> sut.setInterruptMode2((byte) 0));
            var dummy = sut.getRegisters();
            assertThrows(IllegalStateException.class, () -> sut.stop(false));
        });

        doAfterFetch(b -> {
            sut.readFromMemory(address);
            sut.readFromPort(address);
            sut.writeToMemory(address, value);
            sut.writeToPort(address, value);
            sut.setInterruptMode2((byte) 0);
            var dummy = sut.getRegisters();
            sut.stop(false);
        });

        sut.start(null);
    }

    @Test
    public void FetchNextOpcode_can_be_invoked_only_before_instruction_fetch_complete() {
        doBeforeFetch(b -> sut.fetchNextOpcode());

        doAfterFetch(b -> assertThrows(IllegalArgumentException.class, () -> sut.fetchNextOpcode()));

        sut.start(null);
    }

//#endregion

//#region T states management

    @Test
    public void Counts_T_states_for_instruction_execution_and_memory_and_ports_access_appropriately() {
        // TODO some value set cause infinit loop
        var executionStates = fixture.create(Byte.TYPE);
        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        var memoryAccessStates = fixture.create(Byte.TYPE);
        var portAccessStates = fixture.create(Byte.TYPE);
        var memoryAddress = fixture.create(Short.TYPE);
        var portAddress = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);

        Debug.printf("%d, %d, %d, %d, %d, %d, %d", executionStates, m1ReadMemoryStates, memoryAccessStates, portAccessStates, memoryAddress, portAddress, value);

        setStatesReturner(b -> executionStates);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, LD_SP_HL_opcode);
        sut.getMemory().set(2, RET_opcode);

        sut.setMemoryWaitStatesForM1((short) 0, 3, m1ReadMemoryStates);
        sut.setMemoryWaitStatesForNonM1(memoryAddress, 1, memoryAccessStates);
        sut.setPortWaitStates(portAddress, 1, portAccessStates);

        doAfterFetch(b -> {
            if (b == NOP_opcode) {
                sut.readFromMemory(memoryAddress);
                sut.writeToMemory(memoryAddress, value);
                sut.readFromPort(portAddress);
                sut.writeToPort(portAddress, value);
            }
        });

        sut.start(null);

        var expected =
                //3 instructions of 1 byte each executed...
                executionStates * 3 +
                        m1ReadMemoryStates * 3 +
                        //...plus 1 read+1 write to memory + port
                        memoryAccessStates * 2 +
                        portAccessStates * 2;

        assertEquals(expected, sut.getTStatesElapsedSinceReset());
        assertEquals(expected, sut.getTStatesElapsedSinceStart());
    }

    private void setStatesReturner(Function<Byte, Byte> returner) {
        ((FakeInstructionExecutor) sut.getInstructionExecutor()).tStatesReturner = returner;
    }

    @Test
    public void Start_sets_all_TStates_to_zero() {
        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        sut.setMemoryWaitStatesForM1((short) 0, 1, m1ReadMemoryStates);
        var secondRun = new AtomicBoolean(false);

        sut.afterInstructionExecution().addListener(e -> {
            if (!secondRun.get()) {
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceReset());
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceStart());
            }
        });

        sut.start(null);

        sut.beforeInstructionExecution().addListener(e ->
        {
            assertEquals(0, sut.getTStatesElapsedSinceStart());
            assertEquals(0, sut.getTStatesElapsedSinceReset());
        });

        secondRun.set(true);
        sut.start(null);
    }

    @Test
    public void Continue_does_not_modify_TStates() {
        sut.getMemory().set(1, RET_opcode);

        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        sut.setMemoryWaitStatesForM1((short) 0, 2, m1ReadMemoryStates);
        var secondRun = new AtomicBoolean(false);

        sut.afterInstructionExecution().addListener(e -> {
            if (secondRun.get()) {
                assertEquals(m1ReadMemoryStates * 2, sut.getTStatesElapsedSinceReset());
                assertEquals(m1ReadMemoryStates * 2, sut.getTStatesElapsedSinceStart());
            } else {
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceReset());
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceStart());
            }
        });

        sut.start(null);

        secondRun.set(true);
        sut.continue_();
    }

    @Test
    public void Reset_zeroes_TStatesSinceReset_but_not_TStatesSinceStart() {
        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        sut.setMemoryWaitStatesForM1((short) 0, 1, m1ReadMemoryStates);
        var secondRun = new AtomicBoolean(false);

        sut.afterInstructionExecution().addListener(e -> {
            if (secondRun.get()) {
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceReset());
                assertEquals(m1ReadMemoryStates * 2L, sut.getTStatesElapsedSinceStart());
            } else {
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceReset());
                assertEquals((byte) m1ReadMemoryStates, sut.getTStatesElapsedSinceStart());
            }
        });

        sut.start(null);

        secondRun.set(true);
        sut.reset();
        sut.continue_();
    }

    @Test
    public void Reset_sets_StartOfStack_to_0xFFFF() {
        sut.setStartOfStack(fixture.create(Short.TYPE));

        sut.reset();

        assertEquals((short) 0xFFFF, sut.getStartOfStack());
    }

    @Test
    public void ClockSyncHelper_is_notified_of_total_states_after_instruction_execution() {
        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        var executionStates = fixture.create(Byte.TYPE);

        setStatesReturner(b -> executionStates);

        sut.setMemoryWaitStatesForM1((short) 0, 1, m1ReadMemoryStates);

        sut.afterInstructionExecution().addListener(args ->
                verify(clockSyncHelper, times(0)).tryWait(any(Integer.TYPE)));

        sut.start(null);

        verify(clockSyncHelper).tryWait(m1ReadMemoryStates + executionStates);
    }

    @Test
    public void AfterInstructionExecuted_event_contains_proper_Tstates_count() {
        var m1ReadMemoryStates = fixture.create(Byte.TYPE);
        var executionStates = fixture.create(Byte.TYPE);
        var instructionExecuted = new AtomicBoolean(false);

        setStatesReturner(b -> executionStates);

        sut.setMemoryWaitStatesForM1((short) 0, 1, m1ReadMemoryStates);

        sut.afterInstructionExecution().addListener(args -> {
            instructionExecuted.set(true);
            assertEquals(executionStates + m1ReadMemoryStates, args.getTotalTStates());
        });

        sut.start(null);

        assertTrue(instructionExecuted.get());
    }

//#endregion

//#region ExecuteNextInstruction

    @Test
    public void ExecuteNextInstruction_executes_just_one_instruction_and_finishes() {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, NOP_opcode);
        sut.getMemory().set(2, RET_opcode);
        var instructionsExecutedCount = new AtomicInteger(0);

        doBeforeFetch(b -> instructionsExecutedCount.getAndIncrement());

        sut.executeNextInstruction();

        assertEquals(1, instructionsExecutedCount.get());
    }

    @Test
    public void ExecuteNextInstruction_always_sets_StopReason_to_ExecuteNextInstructionInvoked() {
        sut.getMemory().set(0, RET_opcode);
        sut.executeNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, sut.getStopReason());

        sut.getMemory().set(0, DI_opcode);
        sut.reset();
        sut.executeNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, sut.getStopReason());

        doAfterFetch(b -> sut.stop(false));
        sut.reset();
        sut.executeNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, sut.getStopReason());
    }

    @Test
    public void ExecuteNextInstruction_executes_instructions_sequentially() {
        sut.getMemory().set(0, RET_opcode);
        sut.getMemory().set(1, NOP_opcode);
        sut.getMemory().set(2, DI_opcode);

        var executedOpcodes = new ArrayList<Byte>();

        doBeforeFetch(executedOpcodes::add);

        sut.executeNextInstruction();
        sut.executeNextInstruction();
        sut.executeNextInstruction();

        assertArrayEquals(sut.getMemory().getContents(0, 3), toByteArray(executedOpcodes));
    }

    @Test
    public void ExecuteNextInstruction_returns_count_of_elapsed_TStates() {
        var executionStates = fixture.create(Byte.TYPE);
        var M1States = fixture.create(Byte.TYPE);
        var memoryReadStates = fixture.create(Byte.TYPE);
        var address = fixture.create(Short.TYPE);

        sut.setMemoryWaitStatesForM1((short) 0, 1, M1States);
        sut.setMemoryWaitStatesForNonM1(address, 1, memoryReadStates);

        setStatesReturner(b -> executionStates);
        doAfterFetch(b -> sut.readFromMemory(address));

        var actual = sut.executeNextInstruction();
        var expected = executionStates + M1States + memoryReadStates;
        assertEquals(expected, actual);
    }

    @Test
    public void ExecuteNextInstruction_updates_TStatesCounts_appropriately() {
        var statesCount = fixture.create(Byte.TYPE);

        setStatesReturner(b -> statesCount);

        sut.executeNextInstruction();
        assertEquals((byte) statesCount, sut.getTStatesElapsedSinceStart());
        sut.executeNextInstruction();
        assertEquals(statesCount * 2, sut.getTStatesElapsedSinceStart());
    }

//#endregion

//#region FakeInstructionExecutor class

    static class FakeInstructionExecutor implements Z80InstructionExecutor {
        private Z80ProcessorAgent ProcessorAgent;

        @Override
        public Z80ProcessorAgent getProcessorAgent() {
            return ProcessorAgent;
        }

        @Override
        public void setProcessorAgent(Z80ProcessorAgent value) {
            ProcessorAgent = value;
        }

        public Consumer<Byte> extraBeforeFetchCode;

        public Consumer<Byte> getExtraBeforeFetchCode() {
            return extraBeforeFetchCode;
        }

        public void setExtraBeforeFetchCode(Consumer<Byte> value) {
            extraBeforeFetchCode = value;
        }

        public Consumer<Byte> extraAfterFetchCode;

        public Consumer<Byte> getExtraAfterFetchCode() {
            return extraAfterFetchCode;
        }

        public void setExtraAfterFetchCode(Consumer<Byte> value) {
            extraAfterFetchCode = value;
        }

        public Function<Byte, Byte> tStatesReturner;

        public Function<Byte, Byte> getTStatesReturner() {
            return tStatesReturner;
        }

        public void setTStatesReturner(Function<Byte, Byte> value) {
            tStatesReturner = value;
        }

        @Override
        public int execute(byte firstOpcodeByte) {
            if (timesEachInstructionIsExecuted.containsKey(firstOpcodeByte))
                timesEachInstructionIsExecuted.put(firstOpcodeByte, timesEachInstructionIsExecuted.get(firstOpcodeByte) + 1);
            else
                timesEachInstructionIsExecuted.put(firstOpcodeByte, 1);

            if (extraBeforeFetchCode != null)
                extraBeforeFetchCode.accept(firstOpcodeByte);

            instructionFetchFinished().fireEvent(new InstructionFetchFinishedEvent(this) {{
                isLdSpInstruction = (firstOpcodeByte == LD_SP_HL_opcode);
                isRetInstruction = (firstOpcodeByte == RET_opcode);
                isHaltInstruction = (firstOpcodeByte == HALT_opcode);
            }});

            if (extraAfterFetchCode != null)
                extraAfterFetchCode.accept(firstOpcodeByte);

            if (tStatesReturner == null)
                return 0;
            else
                return tStatesReturner.apply(firstOpcodeByte);
        }

        public final Map<Byte, Integer> timesEachInstructionIsExecuted = new HashMap<>();

        final EventHandler<InstructionFetchFinishedEvent> instructionFetchFinished = new EventHandler<>();

        @Override
        public /*event*/ EventHandler<InstructionFetchFinishedEvent> instructionFetchFinished() {
            return instructionFetchFinished;
        }
    }

//#endregion

//#region InstructionFetchFinishedEventNotFiredException

    @Test
    public void Fires_InstructionFetchFinishedEventNotFiredException_if_Execute_returns_without_firing_event() {
        var address = fixture.create(Short.TYPE);
        sut.getMemory().set(address & 0xffff, RET_opcode);
        sut.getRegisters().setPC(address);

        var executor = mock(Z80InstructionExecutor.class);
        when(executor.execute(anyByte())).thenReturn(0);
        when(executor.instructionFetchFinished()).thenReturn(new EventHandler<>());
        sut.setInstructionExecutor(executor);

        var exception = assertThrows(InstructionFetchFinishedEventNotFiredException.class, sut::continue_);

        assertEquals(address, exception.getInstructionAddress());
        assertArrayEquals(new byte[] {RET_opcode}, exception.getFetchedBytes());
    }

//#endregion

//#region PeekNextOpcode

    @Test
    public void PeekNextOpcode_returns_next_opcode_without_increasing_PC_and_without_elapsing_T_states() {
        var executionStates = fixture.create(Byte.TYPE);
        var M1readMemoryStates = fixture.create(Byte.TYPE);
        var memoryAccessStates = fixture.create(Byte.TYPE);
        var memoryAddress = fixture.create(Short.TYPE);

        setStatesReturner(b -> executionStates);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, LD_SP_HL_opcode);
        sut.getMemory().set(2, RET_opcode);

        sut.setMemoryWaitStatesForM1((short) 0, 3, M1readMemoryStates);
        sut.setMemoryWaitStatesForNonM1(memoryAddress, 1, memoryAccessStates);

        var beforeInvoked = new AtomicBoolean(false);

        doBeforeFetch(b -> {
            if (b == LD_SP_HL_opcode) {
                beforeInvoked.set(true);
                for (int i = 0; i < 3; i++) {
                    var oldPC = sut.getRegisters().getPC();
                    assertEquals(RET_opcode, sut.peekNextOpcode());
                    assertEquals(oldPC, sut.getRegisters().getPC());
                }
            }
        });

        doAfterFetch(b -> {
            if (b == NOP_opcode) {
                sut.readFromMemory(memoryAddress);
            }
        });

        sut.start(null);

        assertTrue(beforeInvoked.get());

        var expected =
                //3 instructions of 1 byte each executed...
                executionStates * 3 +
                        M1readMemoryStates * 3 +
                        //...plus 1 read from memory
                        memoryAccessStates;

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);

        assertEquals(expected, sut.getTStatesElapsedSinceReset());
        assertEquals(expected, sut.getTStatesElapsedSinceStart());
    }

    @Test
    public void PeekNextOpcode_can_be_invoked_only_before_instruction_fetch_complete() {
        doBeforeFetch(b -> sut.fetchNextOpcode());

        doAfterFetch(b -> assertThrows(IllegalArgumentException.class, () -> sut.peekNextOpcode()));

        sut.start(null);
    }

    @Test
    public void FetchNextOpcode_after_peek_returns_correct_opcode_and_updates_T_states_appropriately() {
        var executionStates = fixture.create(Byte.TYPE);
        var M1readMemoryStates_0 = fixture.create(Byte.TYPE);
        var M1readMemoryStates_1 = fixture.create(Byte.TYPE);
        var M1readMemoryStates_2 = fixture.create(Byte.TYPE);
        var M1readMemoryStates_3 = fixture.create(Byte.TYPE);
        var secondOpcodeByte = fixture.create(Byte.TYPE);

        setStatesReturner(b -> executionStates);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, LD_SP_HL_opcode);
        sut.getMemory().set(2, secondOpcodeByte);
        sut.getMemory().set(3, RET_opcode);

        sut.setMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates_0);
        sut.setMemoryWaitStatesForM1((short) 1, 1, M1readMemoryStates_1);
        sut.setMemoryWaitStatesForM1((short) 2, 1, M1readMemoryStates_2);
        sut.setMemoryWaitStatesForM1((short) 3, 1, M1readMemoryStates_3);

        var beforeInvoked = new AtomicBoolean(false);

        doBeforeFetch(b -> {
            if (b == LD_SP_HL_opcode) {
                beforeInvoked.set(true);
                assertEquals(secondOpcodeByte, sut.peekNextOpcode());
                assertEquals(2, sut.getRegisters().getPC());
                assertEquals(secondOpcodeByte, sut.fetchNextOpcode());
                assertEquals(3, sut.getRegisters().getPC());
            }
        });

        sut.start(null);

        assertTrue(beforeInvoked.get());

        var expected =
                executionStates * 3 +
                        M1readMemoryStates_0 +
                        M1readMemoryStates_1 +
                        M1readMemoryStates_2 +
                        M1readMemoryStates_3;

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);

        assertEquals(expected, sut.getTStatesElapsedSinceReset());
        assertEquals(expected, sut.getTStatesElapsedSinceStart());
    }

//#endregion
}

