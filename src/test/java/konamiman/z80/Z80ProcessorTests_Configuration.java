package konamiman.z80;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.enums.MemoryAccessMode;
import konamiman.z80.enums.ProcessorState;
import konamiman.z80.enums.StopReason;
import konamiman.z80.impls.ClockSynchronizerImpl;
import konamiman.z80.impls.PlainMemory;
import konamiman.z80.impls.Z80RegistersImpl;
import konamiman.z80.instructions.core.Z80InstructionExecutorImpl;
import konamiman.z80.interfaces.ClockSynchronizer;
import konamiman.z80.interfaces.Memory;
import konamiman.z80.interfaces.Z80InstructionExecutor;
import konamiman.z80.interfaces.Z80Registers;
import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dotnet4j.util.compat.EventHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class Z80ProcessorTests_Configuration {

    private static final int MemorySpaceSize = 65536;
    private static final int PortSpaceSize = 256;

    Z80ProcessorForTests sut;
    JFixture fixture;

    @BeforeEach
    public void Setup() {
        fixture = new JFixture();

        sut = new Z80ProcessorForTests();
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(sut);
    }

    @Test
    public void Has_proper_defaults() {
        assertEquals(4, sut.getClockFrequencyInMHz());
        assertEquals(1, sut.getClockSpeedFactor());

        assertTrue(sut.getAutoStopOnDiPlusHalt());
        assertFalse(sut.getAutoStopOnRetWithStackEmpty());
        assertEquals((short) 0xFFFF, sut.getStartOfStack());

        assertInstanceOf(PlainMemory.class, sut.getMemory());
        assertEquals(65536, sut.getMemory().getSize());
        assertInstanceOf(PlainMemory.class, sut.getPortsSpace());
        assertEquals(256, sut.getPortsSpace().getSize());

        for (int i = 0; i < 65536; i++) {
            assertEquals(MemoryAccessMode.ReadAndWrite, sut.getMemoryAccessMode((short) i));
            assertEquals(0, sut.getMemoryWaitStatesForM1((short) i));
            assertEquals(0, sut.getMemoryWaitStatesForNonM1((short) i));
        }
        for (int i = 0; i < 256; i++) {
            assertEquals(MemoryAccessMode.ReadAndWrite, sut.getPortAccessMode((byte) i));
            assertEquals(0, sut.getPortWaitStates((byte) i));
        }

        assertInstanceOf(Z80RegistersImpl.class, sut.getRegisters());

        assertInstanceOf(Z80InstructionExecutorImpl.class, sut.getInstructionExecutor());
        assertSame(sut, sut.getInstructionExecutor().getProcessorAgent());
        assertInstanceOf(ClockSynchronizerImpl.class, sut.getClockSynchronizer());

        assertEquals(StopReason.NeverRan, sut.getStopReason());
        assertEquals(ProcessorState.Stopped, sut.getState());
        assertFalse(sut.isHalted());
        assertNull(sut.getUserState());
    }

    @Test
    public void Reset_sets_registers_properly() {
        sut.setInstructionExecutionContextToNonNull();

        sut.getRegisters().setIFF1(Bit.ON);
        sut.getRegisters().setIFF1(Bit.ON);
        sut.getRegisters().setPC((short) 1);
        sut.getRegisters().setAF((short) 0);
        sut.getRegisters().setSP((short) 0);
        sut.setInterruptMode((byte) 1);
        sut.setHalted();

        sut.reset();

        assertEquals((short) 0xFFFF, sut.getRegisters().getAF());
        assertEquals((short) 0xFFFF, sut.getRegisters().getSP());
        assertEquals(0, sut.getRegisters().getPC());
        assertEquals(0, sut.getRegisters().getIFF1().intValue());
        assertEquals(0, sut.getRegisters().getIFF2().intValue());
        assertEquals(0, sut.getInterruptMode());

        assertEquals(0, sut.getTStatesElapsedSinceReset());

        assertFalse(sut.isHalted());
    }

    @Test
    public void Interrupt_mode_can_be_set_to_0_1_or_2() {
        sut.setInstructionExecutionContextToNonNull();

        sut.setInterruptMode((byte) 0);
        assertEquals(sut.getInterruptMode(), 0);

        sut.setInterruptMode((byte) 1);
        assertEquals(sut.getInterruptMode(), 1);

        sut.setInterruptMode((byte) 2);
        assertEquals(sut.getInterruptMode(), 2);
    }

    @Test
    public void Interrupt_mode_cannot_be_set_to_higher_than_2() {
        assertThrows(IllegalArgumentException.class, () -> sut.setInterruptMode((byte) 3));
    }

    @Test
    public void SetMemoryAccessMode_and_GetMemoryAccessMode_are_consistent() {
        sut.setMemoryAccessMode((short) 0, 0x4000, MemoryAccessMode.NotConnected);
        sut.setMemoryAccessMode((short) 0x4000, 0x4000, MemoryAccessMode.ReadAndWrite);
        sut.setMemoryAccessMode((short) 0x8000, 0x4000, MemoryAccessMode.ReadOnly);
        sut.setMemoryAccessMode((short) 0xC000, 0x4000, MemoryAccessMode.WriteOnly);

        assertEquals(MemoryAccessMode.NotConnected, sut.getMemoryAccessMode((short) 0));
        assertEquals(MemoryAccessMode.NotConnected, sut.getMemoryAccessMode((short) 0x3FFF));
        assertEquals(MemoryAccessMode.ReadAndWrite, sut.getMemoryAccessMode((short) 0x4000));
        assertEquals(MemoryAccessMode.ReadAndWrite, sut.getMemoryAccessMode((short) 0x7FFF));
        assertEquals(MemoryAccessMode.ReadOnly, sut.getMemoryAccessMode((short) 0x8000));
        assertEquals(MemoryAccessMode.ReadOnly, sut.getMemoryAccessMode((short) 0xBFFF));
        assertEquals(MemoryAccessMode.WriteOnly, sut.getMemoryAccessMode((short) 0xC000));
        assertEquals(MemoryAccessMode.WriteOnly, sut.getMemoryAccessMode((short) 0xFFFF));
    }

    @Test
    public void SetMemoryAccessMode_works_when_address_plus_length_are_on_memory_size_boundary() {
        var value = fixture.create(MemoryAccessMode.class);
        var length = fixture.create(Byte.TYPE);

        sut.setMemoryAccessMode((short) (MemorySpaceSize - length), length, MemoryAccessMode.NotConnected);
    }

    @Test
    public void SetMemoryAccessMode_fails_when_address_plus_length_are_beyond_memory_size_boundary() {
        var value = fixture.create(MemoryAccessMode.class);
        var length = fixture.create(Byte.TYPE);

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sut.setMemoryAccessMode((short) (MemorySpaceSize - length), length + 1, MemoryAccessMode.NotConnected));
    }

    @Test
    public void SetMemoryAccessMode_fails_when_length_is_negative() {
        var value = fixture.create(MemoryAccessMode.class);

        assertThrows(IllegalArgumentException.class, () -> sut.setMemoryAccessMode((short) 0, -1, value));
    }

    @Test
    public void SetPortsSpaceAccessMode_and_GetPortsSpaceAccessMode_are_consistent() {
        sut.setPortsSpaceAccessMode((byte) 0, 64, MemoryAccessMode.NotConnected);
        sut.setPortsSpaceAccessMode((byte) 64, 64, MemoryAccessMode.ReadAndWrite);
        sut.setPortsSpaceAccessMode((byte) 128, 64, MemoryAccessMode.ReadOnly);
        sut.setPortsSpaceAccessMode((byte) 192, 64, MemoryAccessMode.WriteOnly);

        assertEquals(MemoryAccessMode.NotConnected, sut.getPortAccessMode((byte) 0));
        assertEquals(MemoryAccessMode.NotConnected, sut.getPortAccessMode((byte) 63));
        assertEquals(MemoryAccessMode.ReadAndWrite, sut.getPortAccessMode((byte) 64));
        assertEquals(MemoryAccessMode.ReadAndWrite, sut.getPortAccessMode((byte) 127));
        assertEquals(MemoryAccessMode.ReadOnly, sut.getPortAccessMode((byte) 128));
        assertEquals(MemoryAccessMode.ReadOnly, sut.getPortAccessMode((byte) 191));
        assertEquals(MemoryAccessMode.WriteOnly, sut.getPortAccessMode((byte) 192));
        assertEquals(MemoryAccessMode.WriteOnly, sut.getPortAccessMode((byte) 255));
    }

    @Test
    public void SetPortsAccessMode_works_when_address_plus_length_are_on_ports_space_size_boundary() {
        var value = fixture.create(MemoryAccessMode.class);
        var length = fixture.create(Byte.TYPE) & 0xff;

        sut.setPortsSpaceAccessMode((byte) (PortSpaceSize - length), length, MemoryAccessMode.NotConnected);
    }

    @Test
    public void SetPortsAccessMode_fails_when_address_plus_length_are_beyond_ports_space_size_boundary() {
        var value = fixture.create(MemoryAccessMode.class);
        var length = fixture.create(Byte.TYPE) & 0xff;

        assertThrows(IllegalArgumentException.class, () -> sut.setPortsSpaceAccessMode((byte) (PortSpaceSize - length), length + 1, MemoryAccessMode.NotConnected));
    }

    @Test
    public void SetPortsSpaceAccessMode_fails_when_length_is_negative() {
        var value = fixture.create(MemoryAccessMode.class);

        assertThrows(IllegalArgumentException.class, () -> sut.setPortsSpaceAccessMode((byte) 0, -1, value));
    }

    @Test
    public void SetMemoryWaitStatesForM1_and_GetMemoryWaitStatesForM1_are_consistent() {
        var value1 = fixture.create(Byte.TYPE);
        var value2 = fixture.create(Byte.TYPE);

        sut.setMemoryWaitStatesForM1((short) 0, 0x8000, value1);
        sut.setMemoryWaitStatesForM1((short) 0x8000, 0x8000, value2);

        assertEquals(value1, sut.getMemoryWaitStatesForM1((short) 0));
        assertEquals(value1, sut.getMemoryWaitStatesForM1((short) 0x7FFF));
        assertEquals(value2, sut.getMemoryWaitStatesForM1((short) 0x8000));
        assertEquals(value2, sut.getMemoryWaitStatesForM1((short) 0xFFFF));
    }

    @Test
    public void SetMemoryWaitStatesForM1_works_when_address_plus_length_are_in_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        sut.setMemoryWaitStatesForM1((short) (MemorySpaceSize - length), length, value);
    }

    @Test
    public void SetMemoryWaitStatesForM1_fails_when_address_plus_length_are_beyond_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> sut.setMemoryWaitStatesForM1((short) (MemorySpaceSize - length), length + 1, value));
    }

    @Test
    public void SetMemoryWaitStatesForM1_fails_when_length_is_negative() {
        var value = fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class,
                () -> sut.setMemoryWaitStatesForM1((short) 0, -1, value));
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_and_GetMemoryWaitStatesForNonM1_are_consistent() {
        var value1 = fixture.create(Byte.TYPE);
        var value2 = fixture.create(Byte.TYPE);

        sut.setMemoryWaitStatesForNonM1((short) 0, 0x8000, value1);
        sut.setMemoryWaitStatesForNonM1((short) 0x8000, 0x8000, value2);

        assertEquals(value1, sut.getMemoryWaitStatesForNonM1((short) 0));
        assertEquals(value1, sut.getMemoryWaitStatesForNonM1((short) 0x7FFF));
        assertEquals(value2, sut.getMemoryWaitStatesForNonM1((short) 0x8000));
        assertEquals(value2, sut.getMemoryWaitStatesForNonM1((short) 0xFFFF));
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_works_when_address_plus_length_are_in_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        sut.setMemoryWaitStatesForNonM1((short) (MemorySpaceSize - length), length, value);
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_fails_when_address_plus_length_are_beyond_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> sut.setMemoryWaitStatesForNonM1((short) (MemorySpaceSize - length), length + 1, value));
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_fails_when_length_is_negative() {
        var value = fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class,
                () -> sut.setMemoryWaitStatesForNonM1((short) 0, -1, value));
    }

    @Test
    public void SetPortWaitStates_and_GetPortWaitStates_are_consistent() {
        var value1 = fixture.create(Byte.TYPE);
        var value2 = fixture.create(Byte.TYPE);

        sut.setPortWaitStates((short) 0, 128, value1);
        sut.setPortWaitStates((short) 128, 128, value2);

        assertEquals(value1, sut.getPortWaitStates((byte) 0));
        assertEquals(value1, sut.getPortWaitStates((byte) 127));
        assertEquals(value2, sut.getPortWaitStates((byte) 128));
        assertEquals(value2, sut.getPortWaitStates((byte) 255));
    }

    @Test
    public void SetPortWaitStates_works_when_address_plus_length_are_in_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        sut.setPortWaitStates((short) (PortSpaceSize - length), length, value);
    }

    @Test
    public void SetPortWaitStates_fails_when_address_plus_length_are_beyond_memory_size_boundary() {
        var value = fixture.create(Byte.TYPE);
        var length = fixture.create(Byte.TYPE);

        assertThrows(IndexOutOfBoundsException.class,
                () -> sut.setPortWaitStates((short) (PortSpaceSize - length), length + 1, value));
    }

    @Test
    public void SetPortWaitStates_fails_when_length_is_negative() {
        var value = fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class,
                () -> sut.setPortWaitStates((short) 0, -1, value));
    }

    @Test
    public void Can_set_Memory_to_non_null_value() {
        var value = mock(Memory.class);
        sut.setMemory(value);
        assertEquals(value, sut.getMemory());
    }

    @Test
    public void Cannot_set_Memory_to_null() {
        assertThrows(NullPointerException.class, () -> sut.setMemory(null));
    }

    @Test
    public void Can_set_Registers_to_non_null_value() {
        var value = mock(Z80Registers.class);
        sut.setRegisters(value);
        assertEquals(value, sut.getRegisters());
    }

    @Test
    public void Cannot_set_Registers_to_null() {
        assertThrows(NullPointerException.class, () -> sut.setRegisters(null));
    }

    @Test
    public void Can_set_PortsSpace_to_non_null_value() {
        var value = mock(Memory.class);
        sut.setPortsSpace(value);
        assertEquals(value, sut.getPortsSpace());
    }

    @Test
    public void Cannot_set_PortsSpace_to_null() {
        assertThrows(NullPointerException.class, () -> sut.setPortsSpace(null));
    }

    @Test
    public void Can_set_InstructionExecutor_to_non_null_value() {
        var value = mock(Z80InstructionExecutor.class);
        when(value.instructionFetchFinished()).thenReturn(new EventHandler<>());
        sut.setInstructionExecutor(value);
        assertEquals(value, sut.getInstructionExecutor());
    }

    @Test
    public void Cannot_set_InstructionExecutor_to_null() {
        assertThrows(NullPointerException.class, () -> sut.setInstructionExecutor(null));
    }

    @Test
    public void Sets_InstructionExecutor_agent_to_self() {
        var mock = mock(Z80InstructionExecutor.class);
        when(mock.instructionFetchFinished()).thenReturn(new EventHandler<>());
        sut.setInstructionExecutor(mock);

        verify(mock).setProcessorAgent(sut);
    }

    @Test
    public void Can_set_ClockSynchronizationHelper_to_non_null_value() {
        var value = mock(ClockSynchronizer.class);
        sut.setClockSynchronizer(value);
        assertEquals(value, sut.getClockSynchronizer());
    }

    @Test
    public void Can_set_ClockSynchronizationHelper_to_null() {
        sut.setClockSynchronizer(null);
    }

    @Test
    public void Sets_ClockSynchronizationHelper_clockSpeed_to_processor_speed_by_speed_factor() {
        var mock = mock(ClockSynchronizer.class);
        sut.setClockFrequencyInMHz(2);
        sut.setClockSpeedFactor(3);
        sut.setClockSynchronizer(mock);

        verify(mock).setEffectiveClockFrequencyInMHz(2 * 3);
    }

    @Test
    public void Can_set_clock_speed_and_clock_factor_combination_up_to_100_MHz() {
        sut.setClockFrequencyInMHz(20);
        sut.setClockSpeedFactor(5);

        assertEquals(20, sut.getClockFrequencyInMHz());
        assertEquals(5, sut.getClockSpeedFactor());
    }

    @Test
    public void Cannot_set_clock_speed_and_clock_factor_combination_over_100_MHz() {
        sut.setClockFrequencyInMHz(1);
        sut.setClockSpeedFactor(1);

        assertThrows(IllegalArgumentException.class, () ->
        {
            sut.setClockFrequencyInMHz(1);
            sut.setClockSpeedFactor(101);
        });

        assertThrows(IllegalArgumentException.class, () ->
        {
            sut.setClockSpeedFactor(1);
            sut.setClockFrequencyInMHz(101);
        });
    }

    @Test
    public void Can_set_clock_speed_and_clock_factor_combination_down_to_1_KHz() {
        sut.setClockFrequencyInMHz(0.1f);
        sut.setClockSpeedFactor(0.01f);

        assertEquals(0.1f, sut.getClockFrequencyInMHz());
        assertEquals(0.01, sut.getClockSpeedFactor(), 0.001f);
    }

    @Test
    public void Cannot_set_clock_speed_and_clock_factor_combination_under_1_KHz() {
        sut.setClockFrequencyInMHz(1);
        sut.setClockSpeedFactor(1);

        assertThrows(IllegalArgumentException.class, () ->
        {
            sut.setClockFrequencyInMHz(1);
            sut.setClockSpeedFactor(0.0009f);
        });

        assertThrows(IllegalArgumentException.class, () ->
        {
            sut.setClockSpeedFactor(0.0009f);
            sut.setClockFrequencyInMHz(1);
        });
    }
}
