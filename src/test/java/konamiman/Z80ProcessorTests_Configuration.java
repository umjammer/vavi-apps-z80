package konamiman;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DependenciesImplementations.ClockSynchronizer;
import konamiman.DependenciesImplementations.PlainMemory;
import konamiman.DependenciesImplementations.Z80Registers;
import konamiman.DependenciesInterfaces.IClockSynchronizer;
import konamiman.DependenciesInterfaces.IMemory;
import konamiman.DependenciesInterfaces.IZ80InstructionExecutor;
import konamiman.DependenciesInterfaces.IZ80Registers;
import konamiman.Enums.MemoryAccessMode;
import konamiman.Enums.ProcessorState;
import konamiman.Enums.StopReason;
import konamiman.InstructionsExecution.Core.Z80InstructionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class Z80ProcessorTests_Configuration
{
    private static final int MemorySpaceSize = 65536;
    private static final int PortSpaceSize = 256;

    Z80ProcessorForTests Sut; public Z80ProcessorForTests getSut() { return Sut; } public void setSut(Z80ProcessorForTests value) { Sut = value; }
    JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }

    @BeforeEach
    public void Setup()
    {
        Fixture = new JFixture();

        Sut = new Z80ProcessorForTests();
    }

    @Test
    public void Can_create_instances()
    {
        assertNotNull(Sut);
    }

    @Test
    public void Has_proper_defaults()
    {
        assertEquals(4, Sut.getClockFrequencyInMHz());
        assertEquals(1, Sut.getClockSpeedFactor());

        assertTrue(Sut.getAutoStopOnDiPlusHalt());
        assertFalse(Sut.getAutoStopOnRetWithStackEmpty());
        assertEquals(ToShort(0xFFFF), Sut.getStartOfStack());

        assertInstanceOf(PlainMemory.class, Sut.getMemory());
        assertEquals(65536, Sut.getMemory().getSize());
        assertInstanceOf(PlainMemory.class, Sut.getPortsSpace());
        assertEquals(256, Sut.getPortsSpace().getSize());

        for(int i = 0; i < 65536; i++) {
            assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetMemoryAccessMode((short)i));
            assertEquals(0, Sut.GetMemoryWaitStatesForM1((short)i));
            assertEquals(0, Sut.GetMemoryWaitStatesForNonM1((short)i));
        }       
        for(int i = 0; i < 256; i++) {
            assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetPortAccessMode((byte) i));
            assertEquals(0, Sut.GetPortWaitStates((byte) i));
        }

        assertInstanceOf(Z80Registers.class, Sut.getRegisters());

        assertInstanceOf(Z80InstructionExecutor.class, Sut.getInstructionExecutor());
        assertSame(Sut, Sut.getInstructionExecutor().getProcessorAgent());
        assertInstanceOf(ClockSynchronizer.class, Sut.getClockSynchronizer());

        assertEquals(StopReason.NeverRan, Sut.getStopReason());
        assertEquals(ProcessorState.Stopped, Sut.getState());
        assertFalse(Sut.getIsHalted());
        assertNull(Sut.getUserState());
    }

    @Test
    public void Reset_sets_registers_properly()
    {
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.getRegisters().setPC((short) 1);
        Sut.getRegisters().setAF((short) 0);
        Sut.getRegisters().setSP((short) 0);
        Sut.SetInterruptMode((byte) 1);
        Sut.SetIsHalted();
        
        Sut.Reset();

        assertEquals(ToShort(0xFFFF), Sut.getRegisters().getAF());
        assertEquals(ToShort(0xFFFF), Sut.getRegisters().getSP());
        assertEquals(0, Sut.getRegisters().getPC());
        assertEquals(0, Sut.getRegisters().getIFF1());
        assertEquals(0, Sut.getRegisters().getIFF2());
        assertEquals(0, Sut.getInterruptMode());

        assertEquals(0, Sut.getTStatesElapsedSinceReset());

        assertFalse(Sut.getIsHalted());
    }

    @Test
    public void Interrupt_mode_can_be_set_to_0_1_or_2()
    {
        Sut.SetInterruptMode((byte) 0);
        assertEquals(Sut.getInterruptMode(), 0);

        Sut.SetInterruptMode((byte) 1);
        assertEquals(Sut.getInterruptMode(), 1);

        Sut.SetInterruptMode((byte) 2);
        assertEquals(Sut.getInterruptMode(), 2);
    }

    @Test
    public void Interrupt_mode_cannot_be_set_to_higher_than_2()
    {
        assertThrows(IllegalArgumentException.class, () -> Sut.setInterruptMode((byte) 3));
    }

    @Test
    public void SetMemoryAccessMode_and_GetMemoryAccessMode_are_consistent()
    {
        Sut.SetMemoryAccessMode((short) 0, 0x4000, MemoryAccessMode.NotConnected);
        Sut.SetMemoryAccessMode((short) 0x4000, 0x4000, MemoryAccessMode.ReadAndWrite);
        Sut.SetMemoryAccessMode((short) 0x8000, 0x4000, MemoryAccessMode.ReadOnly);
        Sut.SetMemoryAccessMode((short) 0xC000, 0x4000, MemoryAccessMode.WriteOnly);

        assertEquals(MemoryAccessMode.NotConnected, Sut.GetMemoryAccessMode((short) 0));
        assertEquals(MemoryAccessMode.NotConnected, Sut.GetMemoryAccessMode((short) 0x3FFF));
        assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetMemoryAccessMode((short) 0x4000));
        assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetMemoryAccessMode((short) 0x7FFF));
        assertEquals(MemoryAccessMode.ReadOnly, Sut.GetMemoryAccessMode((short) 0x8000));
        assertEquals(MemoryAccessMode.ReadOnly, Sut.GetMemoryAccessMode((short) 0xBFFF));
        assertEquals(MemoryAccessMode.WriteOnly, Sut.GetMemoryAccessMode((short) 0xC000));
        assertEquals(MemoryAccessMode.WriteOnly, Sut.GetMemoryAccessMode((short) 0xFFFF));
    }

    @Test
    public void SetMemoryAccessMode_works_when_address_plus_length_are_on_memory_size_boundary()
    {
       var value = Fixture.create(MemoryAccessMode.class);
       var length = Fixture.create(Byte.TYPE);

       Sut.SetMemoryAccessMode((short)(MemorySpaceSize - length), length, MemoryAccessMode.NotConnected);
    }

    @Test
    public void SetMemoryAccessMode_fails_when_address_plus_length_are_beyond_memory_size_boundary()
    {
        var value = Fixture.create(MemoryAccessMode.class);
        var length = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, () -> Sut.SetMemoryAccessMode((short)(MemorySpaceSize - length), length + 1, MemoryAccessMode.NotConnected));
    }

    @Test
    public void SetMemoryAccessMode_fails_when_length_is_negative()
    {
        var value = Fixture.create(MemoryAccessMode.class);

        assertThrows(IllegalArgumentException.class, () -> Sut.SetMemoryAccessMode((short) 0, -1, value));
    }

    @Test
    public void SetPortsSpaceAccessMode_and_GetPortsSpaceAccessMode_are_consistent()
    {
        Sut.SetPortsSpaceAccessMode((byte) 0, 64, MemoryAccessMode.NotConnected);
        Sut.SetPortsSpaceAccessMode((byte) 64, 64, MemoryAccessMode.ReadAndWrite);
        Sut.SetPortsSpaceAccessMode((byte) 128, 64, MemoryAccessMode.ReadOnly);
        Sut.SetPortsSpaceAccessMode((byte) 192, 64, MemoryAccessMode.WriteOnly);

        assertEquals(MemoryAccessMode.NotConnected, Sut.GetPortAccessMode((byte) 0));
        assertEquals(MemoryAccessMode.NotConnected, Sut.GetPortAccessMode((byte) 63));
        assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetPortAccessMode((byte) 64));
        assertEquals(MemoryAccessMode.ReadAndWrite, Sut.GetPortAccessMode((byte) 127));
        assertEquals(MemoryAccessMode.ReadOnly, Sut.GetPortAccessMode((byte) 128));
        assertEquals(MemoryAccessMode.ReadOnly, Sut.GetPortAccessMode((byte) 191));
        assertEquals(MemoryAccessMode.WriteOnly, Sut.GetPortAccessMode((byte) 192));
        assertEquals(MemoryAccessMode.WriteOnly, Sut.GetPortAccessMode((byte) 255));
    }

    @Test
    public void SetPortsAccessMode_works_when_address_plus_length_are_on_ports_space_size_boundary()
    {
        var value = Fixture.create(MemoryAccessMode.class);
        var length = Fixture.create(Byte.TYPE);

        Sut.SetPortsSpaceAccessMode((byte)(PortSpaceSize - length), length, MemoryAccessMode.NotConnected);
    }

    @Test
    public void SetPortsAccessMode_fails_when_address_plus_length_are_beyond_ports_space_size_boundary()
    {
        var value = Fixture.create(MemoryAccessMode.class);
        var length = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, () -> Sut.SetPortsSpaceAccessMode((byte)(PortSpaceSize - length), length + 1, MemoryAccessMode.NotConnected));
    }

    @Test
    public void SetPortsSpaceAccessMode_fails_when_length_is_negative()
    {
        var value = Fixture.create(MemoryAccessMode.class);

        assertThrows(IllegalArgumentException.class, () -> Sut.SetPortsSpaceAccessMode((byte) 0, -1, value));
    }

    @Test
    public void SetMemoryWaitStatesForM1_and_GetMemoryWaitStatesForM1_are_consistent()
    {
        var value1 = Fixture.create(Byte.TYPE);
        var value2 = Fixture.create(Byte.TYPE);

        Sut.SetMemoryWaitStatesForM1((short) 0, 0x8000, value1);
        Sut.SetMemoryWaitStatesForM1((short) 0x8000, 0x8000, value2);

        assertEquals(value1, Sut.GetMemoryWaitStatesForM1((short)0));
        assertEquals(value1, Sut.GetMemoryWaitStatesForM1((short)0x7FFF));
        assertEquals(value2, Sut.GetMemoryWaitStatesForM1((short)0x8000));
        assertEquals(value2, Sut.GetMemoryWaitStatesForM1((short)0xFFFF));
    }

    @Test
    public void SetMemoryWaitStatesForM1_works_when_address_plus_length_are_in_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        Sut.SetMemoryWaitStatesForM1((short)(MemorySpaceSize - length), length, value);
    }

    @Test
    public void SetMemoryWaitStatesForM1_fails_when_address_plus_length_are_beyond_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetMemoryWaitStatesForM1((short)(MemorySpaceSize - length), length + 1, value));
    }

    @Test
    public void SetMemoryWaitStatesForM1_fails_when_length_is_negative()
    {
        var value = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetMemoryWaitStatesForM1((short) 0, -1, value));
    }
    
    @Test
    public void SetMemoryWaitStatesForNonM1_and_GetMemoryWaitStatesForNonM1_are_consistent()
    {
        var value1 = Fixture.create(Byte.TYPE);
        var value2 = Fixture.create(Byte.TYPE);

        Sut.SetMemoryWaitStatesForNonM1((short)0, 0x8000, value1);
        Sut.SetMemoryWaitStatesForNonM1((short)0x8000, 0x8000, value2);

        assertEquals(value1, Sut.GetMemoryWaitStatesForNonM1((short)0));
        assertEquals(value1, Sut.GetMemoryWaitStatesForNonM1((short)0x7FFF));
        assertEquals(value2, Sut.GetMemoryWaitStatesForNonM1((short)0x8000));
        assertEquals(value2, Sut.GetMemoryWaitStatesForNonM1((short)0xFFFF));
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_works_when_address_plus_length_are_in_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        Sut.SetMemoryWaitStatesForNonM1((short)(MemorySpaceSize - length), length, value);
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_fails_when_address_plus_length_are_beyond_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetMemoryWaitStatesForNonM1((short)(MemorySpaceSize - length), length + 1, value));
    }

    @Test
    public void SetMemoryWaitStatesForNonM1_fails_when_length_is_negative()
    {
        var value = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetMemoryWaitStatesForNonM1((short) 0, -1, value));
    }
    
    @Test
    public void SetPortWaitStates_and_GetPortWaitStates_are_consistent()
    {
        var value1 = Fixture.create(Byte.TYPE);
        var value2 = Fixture.create(Byte.TYPE);

        Sut.SetPortWaitStates((byte) 0, 128, value1);
        Sut.SetPortWaitStates((byte) 128, 128, value2);

        assertEquals(value1, Sut.GetPortWaitStates((byte) 0));
        assertEquals(value1, Sut.GetPortWaitStates((byte) 127));
        assertEquals(value2, Sut.GetPortWaitStates((byte) 128));
        assertEquals(value2, Sut.GetPortWaitStates((byte) 255));
    }

    @Test
    public void SetPortWaitStates_works_when_address_plus_length_are_in_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        Sut.SetPortWaitStates((short)(PortSpaceSize - length), length, value);
    }

    @Test
    public void SetPortWaitStates_fails_when_address_plus_length_are_beyond_memory_size_boundary()
    {
        var value = Fixture.create(Byte.TYPE);
        var length = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetPortWaitStates((short)(PortSpaceSize - length), length + 1, value));
    }

    @Test
    public void SetPortWaitStates_fails_when_length_is_negative()
    {
        var value = Fixture.create(Byte.TYPE);

        assertThrows(IllegalArgumentException.class, 
            () -> Sut.SetPortWaitStates((short) 0, -1, value));
    }

    @Test
    public void Can_set_Memory_to_non_null_value()
    {
        var value = mock(IMemory.class);
        Sut.setMemory(value);
        assertEquals(value, Sut.getMemory());
    }

    @Test
    public void Cannot_set_Memory_to_null()
    {
        assertThrows(NullPointerException.class, () -> Sut.setMemory(null));
    }

    @Test
    public void Can_set_Registers_to_non_null_value()
    {
        var value = mock(IZ80Registers.class);
        Sut.setRegisters(value);
        assertEquals(value, Sut.getRegisters());
    }

    @Test
    public void Cannot_set_Registers_to_null()
    {
        assertThrows(NullPointerException.class, () -> Sut.setRegisters(null));
    }

    @Test
    public void Can_set_PortsSpace_to_non_null_value()
    {
        var value = mock(IMemory.class);
        Sut.setPortsSpace(value);
        assertEquals(value, Sut.getPortsSpace());
    }

    @Test
    public void Cannot_set_PortsSpace_to_null()
    {
        assertThrows(NullPointerException.class, () -> Sut.setPortsSpace(null));
    }

    @Test
    public void Can_set_InstructionExecutor_to_non_null_value()
    {
        var value = mock(IZ80InstructionExecutor.class);
        Sut.setInstructionExecutor(value);
        assertEquals(value, Sut.getInstructionExecutor());
    }

    @Test
    public void Cannot_set_InstructionExecutor_to_null()
    {
        assertThrows(NullPointerException.class, () -> Sut.setInstructionExecutor(null));
    }

    @Test
    public void Sets_InstructionExecutor_agent_to_self()
    {
        var mock = mock(IZ80InstructionExecutor.class);
        Sut.setInstructionExecutor(mock);

        verify(mock).setProcessorAgent(Sut);
    }

    @Test
    public void Can_set_ClockSynchronizationHelper_to_non_null_value()
    {
        var value = mock(IClockSynchronizer.class);
        Sut.setClockSynchronizer(value);
        assertEquals(value, Sut.getClockSynchronizer());
    }

    @Test
    public void Can_set_ClockSynchronizationHelper_to_null()
    {
        Sut.setClockSynchronizer(null);
    }

    @Test
    public void Sets_ClockSynchronizationHelper_clockSpeed_to_processor_speed_by_speed_factor()
    {
        var mock = mock(IClockSynchronizer.class);
        Sut.setClockFrequencyInMHz(2);
        Sut.setClockSpeedFactor(3);
        Sut.setClockSynchronizer(mock);

        verify(mock).setEffectiveClockFrequencyInMHz(2 * 3);
    }

    @Test
    public void Can_set_clock_speed_and_clock_factor_combination_up_to_100_MHz()
    {
        Sut.setClockFrequencyInMHz(20);
        Sut.setClockSpeedFactor(5);

        assertEquals(20, Sut.getClockFrequencyInMHz());
        assertEquals(5, Sut.getClockSpeedFactor());
    }

    @Test
    public void Cannot_set_clock_speed_and_clock_factor_combination_over_100_MHz()
    {
        Sut.setClockFrequencyInMHz(1);
        Sut.setClockSpeedFactor(1);

        assertThrows(IllegalArgumentException.class, () ->
        {
            Sut.setClockFrequencyInMHz(1);
            Sut.setClockSpeedFactor(101);
        });

        assertThrows(IllegalArgumentException.class, () ->
        {
            Sut.setClockSpeedFactor(1);
            Sut.setClockFrequencyInMHz(101);
        });
    }

    @Test
    public void Can_set_clock_speed_and_clock_factor_combination_down_to_1_KHz()
    {
        Sut.setClockFrequencyInMHz(0.1f);
        Sut.setClockSpeedFactor(0.01f);

        assertEquals(0.1f, Sut.getClockFrequencyInMHz());
        assertEquals(0.01, Sut.getClockSpeedFactor());
    }

    @Test
    public void Cannot_set_clock_speed_and_clock_factor_combination_under_1_KHz()
    {
        Sut.setClockFrequencyInMHz(1);
        Sut.setClockSpeedFactor(1);

        assertThrows(IllegalArgumentException.class, () ->
        {
            Sut.setClockFrequencyInMHz(1);
            Sut.setClockSpeedFactor(0.0009f);
        });

        assertThrows(IllegalArgumentException.class, () ->
        {
            Sut.setClockSpeedFactor(0.0009f);
            Sut.setClockFrequencyInMHz(1);
        });
    }
}

