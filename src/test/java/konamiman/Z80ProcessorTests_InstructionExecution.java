package konamiman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import com.flextrade.jfixture.JFixture;
import konamiman.CustomExceptions.InstructionFetchFinishedEventNotFiredException;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DependenciesInterfaces.IClockSynchronizer;
import konamiman.DependenciesInterfaces.IZ80InstructionExecutor;
import konamiman.DependenciesInterfaces.IZ80ProcessorAgent;
import konamiman.Enums.ProcessorState;
import konamiman.Enums.StopReason;
import konamiman.EventArgs.InstructionFetchFinishedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vavi.util.dotnet.EventHandler;

import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.Z80Processor.toByteArray;
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

    private static final byte RET_opcode = (byte)0xC9;
    private static final byte DI_opcode = (byte)0xF3;
    private static final byte HALT_opcode = 0x76;
    private static final byte NOP_opcode = 0x00;
    private static final byte LD_SP_HL_opcode = (byte)0xF9;

    Z80ProcessorForTests Sut; public Z80ProcessorForTests getSut() { return Sut; } public void setSut(Z80ProcessorForTests value) { Sut = value; }
    JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    IClockSynchronizer clockSyncHelper;

    @BeforeEach
    public void Setup() {
        Fixture = new JFixture();

        Sut = new Z80ProcessorForTests();
        Sut.setAutoStopOnRetWithStackEmpty(true);
        Sut.getMemory().set(0, RET_opcode);
        Sut.setMustFailIfNoInstructionFetchComplete(true);

        clockSyncHelper = mock(IClockSynchronizer.class);

        Sut.setInstructionExecutor(new FakeInstructionExecutor() {{
            setProcessorAgent(Sut);
        }});
        Sut.setClockSynchronizer(clockSyncHelper);
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(Sut);
    }

//#region Start, Stop, Pause, Continue

    @Test
    public void Start_does_a_Reset() {
        Sut.getRegisters().setPC(Fixture.create(Short.TYPE));

        Sut.Start(null);

        assertEquals(1, Sut.getRegisters().getPC());
    }

    void DoBeforeFetch(Consumer<Byte> code) {
        ((FakeInstructionExecutor) Sut.getInstructionExecutor()).ExtraBeforeFetchCode = code;
    }

    @Test
    public void Start_sets_StartOfStack_to_0xFFFF() {
        Sut.SetStartOFStack(Fixture.create(Short.TYPE));

        Sut.setAutoStopOnDiPlusHalt(true);
        Sut.getMemory().set(0, DI_opcode);
        Sut.getMemory().set(1, HALT_opcode);

        Sut.Start(null);

        assertEquals(ToShort(0xFFFF), Sut.getStartOfStack());
    }

    @Test
    public void Starts_sets_global_state_if_passed_as_not_null() {
        var state = Fixture.create(Object.class);
        Sut.setUserState(null);

        Sut.Start(state);

        assertSame(state, Sut.getUserState());
    }

    @Test
    public void Starts_does_not_set_global_state_if_passed_as_null() {
        Sut.setUserState(Fixture.create(Object.class));

        Sut.Start(null);

        assertNotNull(Sut.getUserState());
    }

    @Test
    public void Continue_sets_execution_context_and_does_not_reset() {
        var pc = Fixture.create(Short.TYPE);
        Sut.getRegisters().setPC(pc);
        Sut.getMemory().set(pc, RET_opcode);
        Sut.SetStartOFStack(Sut.getRegisters().getSP());

        Sut.Continue();

        assertEquals(Inc(pc), Sut.getRegisters().getPC());
    }

    @Test
    public void Start_sets_ProcessorState_to_running() {
        DoBeforeFetch(b -> assertEquals(ProcessorState.Running, Sut.getState()));

        Sut.Start(null);
    }

    private void AssertExecuted(byte opcode, int times) {
        var dictionary = ((FakeInstructionExecutor) Sut.getInstructionExecutor()).TimesEachInstructionIsExecuted;
        if (times == 0)
            assertFalse(dictionary.containsKey(opcode));
        else
            assertEquals(dictionary.get(opcode), times);
    }

    @Test
    public void StopRequest_stops_execution() {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, DI_opcode);
        Sut.getMemory().set(2, RET_opcode);

        DoAfterFetch(b -> {
            if (b == DI_opcode) Sut.Stop(false);
        });

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(StopReason.StopInvoked, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
    }

    @Test
    public void PauseRequest_stops_execution() {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, DI_opcode);
        Sut.getMemory().set(2, RET_opcode);

        DoAfterFetch(b -> {
            if (b == DI_opcode) Sut.Stop(/*isPause:*/ true);
        });

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(StopReason.PauseInvoked, Sut.getStopReason());
        assertEquals(ProcessorState.Paused, Sut.getState());
    }

    @Test
    public void Cannot_stop_if_no_execution_context() {
        assertThrows(UnsupportedOperationException.class, () -> Sut.Stop(false));
    }

    @Test
    public void StopReason_is_not_applicable_while_executing() {
        DoBeforeFetch(b -> assertEquals(StopReason.NotApplicable, Sut.getStopReason()));

        Sut.Start(null);
    }

    @Test
    public void Has_proper_state_after_unhandled_exception() {
        DoBeforeFetch(b -> {
            throw new RuntimeException();
        });

        assertThrows(RuntimeException.class, () -> Sut.Start(null));

        assertEquals(ProcessorState.Stopped, Sut.getState());
        assertEquals(StopReason.ExceptionThrown, Sut.getStopReason());
    }

//#endregion

//#region Conditions at runtime

    @Test
    public void Execution_invokes_InstructionExecutor_for_each_fetched_opcode() {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, DI_opcode);
        Sut.getMemory().set(2, RET_opcode);

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(RET_opcode, 1);
    }

    @Test
    public void Cannot_change_interrupt_mode_from_agent_interface_if_no_execution_context() {
        assertThrows(UnsupportedOperationException.class, () -> Sut.SetInterruptMode((byte) 0));
    }

    @Test
    public void Can_change_interrupt_mode() {
        Sut.setInterruptMode((byte) 0);

        DoAfterFetch(b -> Sut.SetInterruptMode((byte) 2));

        Sut.Start(null);

        assertEquals(2, Sut.getInterruptMode());
    }

    void DoAfterFetch(Consumer<Byte> code) {
        ((FakeInstructionExecutor) Sut.getInstructionExecutor()).ExtraAfterFetchCode = code;
    }

//#endregion

//#region Auto-stop conditions

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Auto_stops_when_HALT_on_DI_found_or_when_RET_with_initial_stack_is_found_if_configured_to_do_so(boolean autoStopOnDiPlusHalt) {
        Sut.setAutoStopOnDiPlusHalt(autoStopOnDiPlusHalt);
        Sut.setAutoStopOnRetWithStackEmpty(!autoStopOnDiPlusHalt);

        Sut.getMemory().set(0, DI_opcode);
        Sut.getMemory().set(1, autoStopOnDiPlusHalt ? HALT_opcode : NOP_opcode);
        Sut.getMemory().set(2, RET_opcode);

        DoBeforeFetch(b -> Sut.getRegisters().setIFF1(Bit.OFF));

        Sut.Start(null);

        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, autoStopOnDiPlusHalt ? 1 : 0);
        AssertExecuted(RET_opcode, autoStopOnDiPlusHalt ? 0 : 1);

        assertEquals(autoStopOnDiPlusHalt ? StopReason.DiPlusHalt : StopReason.RetWithStackEmpty, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
    }

    @Test
    public void Does_not_auto_stop_when_HALT_on_EI_found_regardless_of_AutoStopOnDiPlusHalt_is_true() {
        Sut.setAutoStopOnDiPlusHalt(true);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.getMemory().set(0, DI_opcode);
        Sut.getMemory().set(1, HALT_opcode);
        Sut.getMemory().set(2, RET_opcode);

        DoBeforeFetch(b -> Sut.getRegisters().setIFF1(Bit.ON));

        var instructionsExecutedCount = new AtomicInteger(0);

        Sut.AfterInstructionExecution().addListener(args -> {
            if (instructionsExecutedCount.get() == 5)
                args.getExecutionStopper().Stop(false);
            else
                instructionsExecutedCount.getAndIncrement();
        });

        Sut.Start(null);

        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 1);
        AssertExecuted(RET_opcode, 0);

        assertEquals(5, instructionsExecutedCount.get());
        assertEquals(StopReason.StopInvoked, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
    }

    @Test
    public void Auto_stops_when_RET_is_found_with_stack_equal_to_initial_value_if_AutoStopOnRetWithStackEmpty_is_true() {
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.getMemory().set(0, LD_SP_HL_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(2, DI_opcode);

        var spValue = Fixture.create(Short.TYPE);

        DoBeforeFetch(b -> Sut.getRegisters().setIFF1(Bit.ON));
        DoAfterFetch(b -> {
            if (b == LD_SP_HL_opcode) Sut.getRegisters().setSP(spValue);
        });

        Sut.Start(null);

        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);
        AssertExecuted(DI_opcode, 0);

        assertEquals(StopReason.RetWithStackEmpty, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
    }

    @Test
    public void LD_SP_instructions_change_value_of_StartOfStack() {
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.getMemory().set(0, LD_SP_HL_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.SetStartOFStack(Fixture.create(Short.TYPE));

        var spValue = Fixture.create(Short.TYPE);

        DoAfterFetch(b -> {
            if (b == LD_SP_HL_opcode) Sut.getRegisters().setSP(spValue);
        });

        Sut.Start(null);

        AssertExecuted(LD_SP_HL_opcode, 1);
        AssertExecuted(RET_opcode, 1);

        assertEquals(StopReason.RetWithStackEmpty, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
        assertEquals(spValue, Sut.getStartOfStack());
    }

    @Test
    public void Does_not_auto_stops_when_RET_is_found_with_stack_not_equal_to_initial_value_if_AutoStopOnRetWithStackEmpty_is_true() {
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(2, RET_opcode);
        Sut.getMemory().set(3, DI_opcode);

        var spValue = Fixture.create(Short.TYPE);

        DoBeforeFetch(b -> Sut.getRegisters().setIFF1(Bit.ON));

        DoAfterFetch(b -> {
            if (b == NOP_opcode)
                Sut.getRegisters().addSP((short) 2);
            else if (b == RET_opcode)
                Sut.getRegisters().subSP((short) 2);
        });

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(RET_opcode, 2);
        AssertExecuted(DI_opcode, 0);

        assertEquals(StopReason.RetWithStackEmpty, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
    }

//#endregion

//#region Before and after instruction execution events

    @Test
    public void Fires_before_and_after_instruction_execution_with_proper_opcodes_and_local_state() {
        var executeInvoked = new AtomicBoolean(false);
        var beforeFetchEventRaised = new AtomicBoolean(false);
        var beforeExecutionEventRaised = new AtomicBoolean(false);
        var afterEventRaised = new AtomicBoolean(false);
        var localState = Fixture.create(Object.class);

        var instructionBytes = new byte[] {
                RET_opcode, HALT_opcode, DI_opcode, NOP_opcode
        };
        Sut.getMemory().SetContents(0, instructionBytes, 0, 0);

        DoBeforeFetch(b -> {
            Sut.fetchNextOpcode();
            Sut.fetchNextOpcode();
            Sut.fetchNextOpcode();
        });

        Sut.BeforeInstructionFetch().addListener(e -> {
            beforeFetchEventRaised.set(true);
            assertFalse(executeInvoked.get());
            assertFalse(beforeExecutionEventRaised.get());
            assertFalse(afterEventRaised.get());
            executeInvoked.set(false);
            assertNull(e.getLocalUserState());

            e.setLocalUserState(localState);
        });

        Sut.BeforeInstructionExecution().addListener(e -> {
            beforeExecutionEventRaised.set(true);
            assertFalse(executeInvoked.get());
            assertTrue(beforeExecutionEventRaised.get());
            assertFalse(afterEventRaised.get());
            executeInvoked.set(true);
            assertEquals(instructionBytes, e.getOpcode());
            assertEquals(localState, e.getLocalUserState());
        });

        Sut.AfterInstructionExecution().addListener(e -> {
            afterEventRaised.set(true);
            assertTrue(executeInvoked.get());
            assertTrue(beforeFetchEventRaised.get());
            assertTrue(beforeExecutionEventRaised.get());
            assertEquals(instructionBytes, e.getOpcode());
            assertEquals(localState, e.getLocalUserState());
        });

        Sut.Start(null);

        assertTrue(beforeFetchEventRaised.get());
        assertTrue(beforeExecutionEventRaised.get());
        assertTrue(afterEventRaised.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Stops_execution_if_requested_from_AfterInstructionExecutionEvent(boolean isPause) {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, DI_opcode);
        Sut.getMemory().set(2, HALT_opcode);
        Sut.getMemory().set(3, RET_opcode);

        Sut.AfterInstructionExecution().addListener(e -> {
            if (e.getOpcode()[0] == DI_opcode)
                e.getExecutionStopper().Stop(isPause);
        });

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 0);
        AssertExecuted(RET_opcode, 0);

        assertEquals(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked, Sut.getStopReason());
        assertEquals(isPause ? ProcessorState.Paused : ProcessorState.Stopped, Sut.getState());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Stops_execution_if_requested_from_BeforeInstructionFetchEvent(boolean isPause) {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, DI_opcode);
        Sut.getMemory().set(2, HALT_opcode);
        Sut.getMemory().set(3, RET_opcode);

        Sut.BeforeInstructionFetch().addListener(e -> {
            if (Sut.getRegisters().getPC() == 2)
                e.getExecutionStopper().Stop(isPause);
        });

        Sut.Start(null);

        AssertExecuted(NOP_opcode, 1);
        AssertExecuted(DI_opcode, 1);
        AssertExecuted(HALT_opcode, 0);
        AssertExecuted(RET_opcode, 0);

        assertEquals(isPause ? StopReason.PauseInvoked : StopReason.StopInvoked, Sut.getStopReason());
        assertEquals(isPause ? ProcessorState.Paused : ProcessorState.Stopped, Sut.getState());
    }

//#endregion

//#region Invoking agent members at the right time

    @Test
    public void ProcessorAgent_members_other_than_FetchNextOpcode_and_Registers_can_be_invoked_only_after_instruction_fetch_complete() {
        var address = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);

        Sut.setMustFailIfNoInstructionFetchComplete(true);

        DoBeforeFetch(b -> {
            assertThrows(UnsupportedOperationException.class, () -> Sut.readFromMemory(address));
            assertThrows(UnsupportedOperationException.class, () -> Sut.ReadFromPort(address));
            assertThrows(UnsupportedOperationException.class, () -> Sut.WriteToMemory(address, value));
            assertThrows(UnsupportedOperationException.class, () -> Sut.WriteToPort(address, value));
            assertThrows(UnsupportedOperationException.class, () -> Sut.SetInterruptMode((byte) 0));
            var dummy = ((IZ80ProcessorAgent) Sut).getRegisters();
            assertThrows(UnsupportedOperationException.class, () -> Sut.Stop(false));
        });

        DoAfterFetch(b -> {
            Sut.readFromMemory(address);
            Sut.ReadFromPort(address);
            Sut.WriteToMemory(address, value);
            Sut.WriteToPort(address, value);
            Sut.SetInterruptMode((byte) 0);
            var dummy = ((IZ80ProcessorAgent) Sut).getRegisters();
            Sut.Stop(false);
        });

        Sut.Start(null);
    }

    @Test
    public void FetchNextOpcode_can_be_invoked_only_before_instruction_fetch_complete() {
        DoBeforeFetch(b -> Sut.fetchNextOpcode());

        DoAfterFetch(b -> assertThrows(UnsupportedOperationException.class, () -> Sut.fetchNextOpcode()));

        Sut.Start(null);
    }

//#endregion

//#region T states management

    @Test
    public void Counts_T_states_for_instruction_execution_and_memory_and_ports_access_appropriately() {
        var executionStates = Fixture.create(Byte.TYPE);
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        var memoryAccessStates = Fixture.create(Byte.TYPE);
        var portAccessStates = Fixture.create(Byte.TYPE);
        var memoryAddress = Fixture.create(Short.TYPE);
        var portAddress = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);

        SetStatesReturner(b -> executionStates);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, LD_SP_HL_opcode);
        Sut.getMemory().set(2, RET_opcode);

        Sut.SetMemoryWaitStatesForM1((short) 0, 3, M1readMemoryStates);
        Sut.SetMemoryWaitStatesForNonM1(memoryAddress, 1, memoryAccessStates);
        Sut.SetPortWaitStates(portAddress, 1, portAccessStates);

        DoAfterFetch(b -> {
            if (b == NOP_opcode) {
                Sut.readFromMemory(memoryAddress);
                Sut.WriteToMemory(memoryAddress, value);
                Sut.ReadFromPort(portAddress);
                Sut.WriteToPort(portAddress, value);
            }
        });

        Sut.Start(null);

        var expected =
                //3 instructions of 1 byte each executed...
                executionStates * 3 +
                M1readMemoryStates * 3 +
                //...plus 1 read+1 write to memory + port
                memoryAccessStates * 2 +
                portAccessStates * 2;

        assertEquals(expected, Sut.getTStatesElapsedSinceReset());
        assertEquals(expected, Sut.getTStatesElapsedSinceStart());
    }

    private void SetStatesReturner(Function<Byte, Byte> returner) {
        ((FakeInstructionExecutor) Sut.getInstructionExecutor()).TStatesReturner = returner;
    }

    @Test
    public void Start_sets_all_TStates_to_zero() {
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates);
        var secondRun = new AtomicBoolean(false);

        Sut.AfterInstructionExecution().addListener(e -> {
            if (!secondRun.get()) {
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceReset());
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceStart());
            }
        });

        Sut.Start(null);

        Sut.BeforeInstructionExecution().addListener(e ->
        {
            assertEquals(0, Sut.getTStatesElapsedSinceStart());
            assertEquals(0, Sut.getTStatesElapsedSinceReset());
        });

        secondRun.set(true);
        Sut.Start(null);
    }

    @Test
    public void Continue_does_not_modify_TStates() {
        Sut.getMemory().set(1, RET_opcode);

        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        Sut.SetMemoryWaitStatesForM1((short) 0, 2, M1readMemoryStates);
        var secondRun = new AtomicBoolean(false);

        Sut.AfterInstructionExecution().addListener(e -> {
            if (secondRun.get()) {
                assertEquals(M1readMemoryStates * 2, Sut.getTStatesElapsedSinceReset());
                assertEquals(M1readMemoryStates * 2, Sut.getTStatesElapsedSinceStart());
            } else {
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceReset());
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceStart());
            }
        });

        Sut.Start(null);

        secondRun.set(true);
        Sut.Continue();
    }

    @Test
    public void Reset_zeroes_TStatesSinceReset_but_not_TStatesSinceStart() {
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates);
        var secondRun = new AtomicBoolean(false);

        Sut.AfterInstructionExecution().addListener(e -> {
            if (secondRun.get()) {
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceReset());
                assertEquals(M1readMemoryStates * 2L, Sut.getTStatesElapsedSinceStart());
            } else {
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceReset());
                assertEquals((byte) M1readMemoryStates, Sut.getTStatesElapsedSinceStart());
            }
        });

        Sut.Start(null);

        secondRun.set(true);
        Sut.Reset();
        Sut.Continue();
    }

    @Test
    public void Reset_sets_StartOfStack_to_0xFFFF() {
        Sut.SetStartOFStack(Fixture.create(Short.TYPE));

        Sut.Reset();

        assertEquals(ToShort(0xFFFF), Sut.getStartOfStack());
    }

    @Test
    public void ClockSyncHelper_is_notified_of_total_states_after_instruction_execution() {
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        var executionStates = Fixture.create(Byte.TYPE);

        SetStatesReturner(b -> executionStates);

        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates);

        Sut.AfterInstructionExecution().addListener(args ->
                verify(clockSyncHelper, times(0)).TryWait(any(Integer.TYPE)));

        Sut.Start(null);

        verify(clockSyncHelper).TryWait(M1readMemoryStates + executionStates);
    }

    @Test
    public void AfterInstructionExecuted_event_contains_proper_Tstates_count() {
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        var executionStates = Fixture.create(Byte.TYPE);
        var instructionExecuted = new AtomicBoolean(false);

        SetStatesReturner(b -> executionStates);

        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates);

        Sut.AfterInstructionExecution().addListener(args -> {
            instructionExecuted.set(true);
            assertEquals(executionStates + M1readMemoryStates, args.getTotalTStates());
        });

        Sut.Start(null);

        assertTrue(instructionExecuted.get());
    }

//#endregion

//#region ExecuteNextInstruction

    @Test
    public void ExecuteNextInstruction_executes_just_one_instruction_and_finishes() {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, NOP_opcode);
        Sut.getMemory().set(2, RET_opcode);
        var instructionsExecutedCount = new AtomicInteger(0);

        DoBeforeFetch(b -> instructionsExecutedCount.getAndIncrement());

        Sut.ExecuteNextInstruction();

        assertEquals(1, instructionsExecutedCount.get());
    }

    @Test
    public void ExecuteNextInstruction_always_sets_StopReason_to_ExecuteNextInstructionInvoked() {
        Sut.getMemory().set(0, RET_opcode);
        Sut.ExecuteNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, Sut.getStopReason());

        Sut.getMemory().set(0, DI_opcode);
        Sut.Reset();
        Sut.ExecuteNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, Sut.getStopReason());

        DoAfterFetch(b -> Sut.Stop(false));
        Sut.Reset();
        Sut.ExecuteNextInstruction();
        assertEquals(StopReason.ExecuteNextInstructionInvoked, Sut.getStopReason());
    }

    @Test
    public void ExecuteNextInstruction_executes_instructions_sequentially() {
        Sut.getMemory().set(0, RET_opcode);
        Sut.getMemory().set(1, NOP_opcode);
        Sut.getMemory().set(2, DI_opcode);

        var executedOpcodes = new ArrayList<Byte>();

        DoBeforeFetch(executedOpcodes::add);

        Sut.ExecuteNextInstruction();
        Sut.ExecuteNextInstruction();
        Sut.ExecuteNextInstruction();

        assertEquals(Sut.getMemory().getContents(0, 3), toByteArray(executedOpcodes));
    }

    @Test
    public void ExecuteNextInstruction_returns_count_of_elapsed_TStates() {
        var executionStates = Fixture.create(Byte.TYPE);
        var M1States = Fixture.create(Byte.TYPE);
        var memoryReadStates = Fixture.create(Byte.TYPE);
        var address = Fixture.create(Short.TYPE);

        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1States);
        Sut.SetMemoryWaitStatesForNonM1(address, 1, memoryReadStates);

        SetStatesReturner(b -> executionStates);
        DoAfterFetch(b -> Sut.readFromMemory(address));

        var actual = Sut.ExecuteNextInstruction();
        var expected = executionStates + M1States + memoryReadStates;
        assertEquals(expected, actual);
    }

    @Test
    public void ExecuteNextInstruction_updates_TStatesCounts_appropriately() {
        var statesCount = Fixture.create(Byte.TYPE);

        SetStatesReturner(b -> statesCount);

        Sut.ExecuteNextInstruction();
        assertEquals((byte) statesCount, Sut.getTStatesElapsedSinceStart());
        Sut.ExecuteNextInstruction();
        assertEquals(statesCount * 2, Sut.getTStatesElapsedSinceStart());
    }

//#endregion

//#region FakeInstructionExecutor class

    private static class FakeInstructionExecutor implements IZ80InstructionExecutor {
        private IZ80ProcessorAgent ProcessorAgent;

        public IZ80ProcessorAgent getProcessorAgent() {
            return ProcessorAgent;
        }

        public void setProcessorAgent(IZ80ProcessorAgent value) {
            ProcessorAgent = value;
        }

        public Consumer<Byte> ExtraBeforeFetchCode;

        public Consumer<Byte> getExtraBeforeFetchCode() {
            return ExtraBeforeFetchCode;
        }

        public void setExtraBeforeFetchCode(Consumer<Byte> value) {
            ExtraBeforeFetchCode = value;
        }

        public Consumer<Byte> ExtraAfterFetchCode;

        public Consumer<Byte> getExtraAfterFetchCode() {
            return ExtraAfterFetchCode;
        }

        public void setExtraAfterFetchCode(Consumer<Byte> value) {
            ExtraAfterFetchCode = value;
        }

        public Function<Byte, Byte> TStatesReturner;

        public Function<Byte, Byte> getTStatesReturner() {
            return TStatesReturner;
        }

        public void setTStatesReturner(Function<Byte, Byte> value) {
            TStatesReturner = value;
        }

        public int execute(byte firstOpcodeByte) {
            if (TimesEachInstructionIsExecuted.containsKey(firstOpcodeByte))
                TimesEachInstructionIsExecuted.put(firstOpcodeByte, TimesEachInstructionIsExecuted.get(firstOpcodeByte) + 1);
            else
                TimesEachInstructionIsExecuted.put(firstOpcodeByte, 1);

            if (ExtraBeforeFetchCode != null)
                ExtraBeforeFetchCode.accept(firstOpcodeByte);

            InstructionFetchFinished().fireEvent(new InstructionFetchFinishedEvent(this) {{
                IsLdSpInstruction = (firstOpcodeByte == LD_SP_HL_opcode);
                IsRetInstruction = (firstOpcodeByte == RET_opcode);
                IsHaltInstruction = (firstOpcodeByte == HALT_opcode);
            }});

            if (ExtraAfterFetchCode != null)
                ExtraAfterFetchCode.accept(firstOpcodeByte);

            if (TStatesReturner == null)
                return 0;
            else
                return TStatesReturner.apply(firstOpcodeByte);
        }

        public Map<Byte, Integer> TimesEachInstructionIsExecuted = new HashMap<>();

        EventHandler<InstructionFetchFinishedEvent> _InstructionFetchFinished = new EventHandler<>();

        public /*event*/ EventHandler<InstructionFetchFinishedEvent> InstructionFetchFinished() {
            return _InstructionFetchFinished;
        }
    }

//#endregion

//#region InstructionFetchFinishedEventNotFiredException

    @Test
    public void Fires_InstructionFetchFinishedEventNotFiredException_if_Execute_returns_without_firing_event() {
        var address = Fixture.create(Short.TYPE);
        Sut.getMemory().set(address, RET_opcode);
        Sut.getRegisters().setPC(address);

        var executor = mock(IZ80InstructionExecutor.class);
        when(executor.execute(anyByte())).thenReturn(0);
        Sut.setInstructionExecutor(executor);

        var exception = assertThrows(InstructionFetchFinishedEventNotFiredException.class, Sut::Continue);

        assertEquals(address, exception.getInstructionAddress());
        assertEquals(new byte[] {RET_opcode}, exception.getFetchedBytes());
    }

//#endregion

//#region PeekNextOpcode

    @Test
    public void PeekNextOpcode_returns_next_opcode_without_increasing_PC_and_without_elapsing_T_states() {
        var executionStates = Fixture.create(Byte.TYPE);
        var M1readMemoryStates = Fixture.create(Byte.TYPE);
        var memoryAccessStates = Fixture.create(Byte.TYPE);
        var memoryAddress = Fixture.create(Short.TYPE);

        SetStatesReturner(b -> executionStates);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, LD_SP_HL_opcode);
        Sut.getMemory().set(2, RET_opcode);

        Sut.SetMemoryWaitStatesForM1((short) 0, 3, M1readMemoryStates);
        Sut.SetMemoryWaitStatesForNonM1(memoryAddress, 1, memoryAccessStates);

        var beforeInvoked = new AtomicBoolean(false);

        DoBeforeFetch(b -> {
            if (b == LD_SP_HL_opcode) {
                beforeInvoked.set(true);
                for (int i = 0; i < 3; i++) {
                    var oldPC = Sut.getRegisters().getPC();
                    assertEquals(RET_opcode, Sut.PeekNextOpcode());
                    assertEquals(oldPC, Sut.getRegisters().getPC());
                }
            }
        });

        DoAfterFetch(b -> {
            if (b == NOP_opcode) {
                Sut.readFromMemory(memoryAddress);
            }
        });

        Sut.Start(null);

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

        assertEquals(expected, Sut.getTStatesElapsedSinceReset());
        assertEquals(expected, Sut.getTStatesElapsedSinceStart());
    }

    @Test
    public void PeekNextOpcode_can_be_invoked_only_before_instruction_fetch_complete() {
        DoBeforeFetch(b -> Sut.fetchNextOpcode());

        DoAfterFetch(b -> assertThrows(UnsupportedOperationException.class, () -> Sut.PeekNextOpcode()));

        Sut.Start(null);
    }

    @Test
    public void FetchNextOpcode_after_peek_returns_correct_opcode_and_updates_T_states_appropriately() {
        var executionStates = Fixture.create(Byte.TYPE);
        var M1readMemoryStates_0 = Fixture.create(Byte.TYPE);
        var M1readMemoryStates_1 = Fixture.create(Byte.TYPE);
        var M1readMemoryStates_2 = Fixture.create(Byte.TYPE);
        var M1readMemoryStates_3 = Fixture.create(Byte.TYPE);
        var secondOpcodeByte = Fixture.create(Byte.TYPE);

        SetStatesReturner(b -> executionStates);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, LD_SP_HL_opcode);
        Sut.getMemory().set(2, secondOpcodeByte);
        Sut.getMemory().set(3, RET_opcode);

        Sut.SetMemoryWaitStatesForM1((short) 0, 1, M1readMemoryStates_0);
        Sut.SetMemoryWaitStatesForM1((short) 1, 1, M1readMemoryStates_1);
        Sut.SetMemoryWaitStatesForM1((short) 2, 1, M1readMemoryStates_2);
        Sut.SetMemoryWaitStatesForM1((short) 3, 1, M1readMemoryStates_3);

        var beforeInvoked = new AtomicBoolean(false);

        DoBeforeFetch(b ->
        {
            if (b == LD_SP_HL_opcode) {
                beforeInvoked.set(true);
                assertEquals(secondOpcodeByte, Sut.PeekNextOpcode());
                assertEquals(2, Sut.getRegisters().getPC());
                assertEquals(secondOpcodeByte, Sut.fetchNextOpcode());
                assertEquals(3, Sut.getRegisters().getPC());
            }
        });

        Sut.Start(null);

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

        assertEquals(expected, Sut.getTStatesElapsedSinceReset());
        assertEquals(expected, Sut.getTStatesElapsedSinceStart());
    }

//#endregion
}

