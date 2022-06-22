package konamiman;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.flextrade.jfixture.JFixture;
import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import konamiman.Enums.MemoryAccessEventType;
import konamiman.Enums.StopReason;
import konamiman.EventArgs.MemoryAccessEventArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class Z80ProcessorTests_Interrupts
{
    private static final byte RET_opcode = (byte) 0xC9;
    private static final byte DI_opcode = (byte)0xF3;
    private static final byte EI_opcode = (byte)0xFB;
    private static final byte HALT_opcode = 0x76;
    private static final byte NOP_opcode = 0x00;
    private static final byte RST20h_opcode = (byte)0xE7;

    Z80ProcessorForTests Sut; public Z80ProcessorForTests getSut() { return Sut; } public void setSut(Z80ProcessorForTests value) { Sut = value; }
    JFixture Fixture; public JFixture getFixture() { return Fixture; } public void setFixture(JFixture value) { Fixture = value; }
    InterruptSourceForTests InterruptSource1; public InterruptSourceForTests getInterruptSource1() { return InterruptSource1; } public void setInterruptSource1(InterruptSourceForTests value) { InterruptSource1 = value; }
    InterruptSourceForTests InterruptSource2; public InterruptSourceForTests getInterruptSource2() { return InterruptSource2; } public void setInterruptSource2(InterruptSourceForTests value) { InterruptSource2 = value; }

    @BeforeEach
    public void Setup()
    {
        Fixture = new JFixture();

        Sut = new Z80ProcessorForTests();
        Sut.SetInstructionExecutionContextToNonNull();

        InterruptSource1 = new InterruptSourceForTests();
        InterruptSource2 = new InterruptSourceForTests();
    }

    @Test
    public void Can_create_instances()
    {
        assertNotNull(Sut);
    }

//#region Interrupt source registration

    @Test
    public void RegisterInterruptSource_and_GetRegisteredSources_are_simmetrical()
    {
        Sut.RegisterInterruptSource(InterruptSource1);
        Sut.RegisterInterruptSource(InterruptSource2);

        var expected = new Object[] {InterruptSource1, InterruptSource2};
        var actual = Sut.GetRegisteredInterruptSources();
        assertArrayEquals(expected, actual.toArray());

    }

    @Test
    public void RegisterInterruptSource_does_not_register_same_instance_twice()
    {
        Sut.RegisterInterruptSource(InterruptSource1);
        Sut.RegisterInterruptSource(InterruptSource1);

        var expected = new Object[] {InterruptSource1};
        var actual = Sut.GetRegisteredInterruptSources();
        assertArrayEquals(expected, actual.toArray());
    }

    @Test
    public void UnregisterAllInterruptSources_clears_sources_list()
    {
        Sut.RegisterInterruptSource(InterruptSource1);
        Sut.RegisterInterruptSource(InterruptSource2);

        Sut.UnregisterAllInterruptSources();

        var actual = Sut.GetRegisteredInterruptSources();
        assertTrue(actual.isEmpty());
    }

//#endregion

//#region Accepting NMI interrupts

    @Test
    public void Nmi_is_accepted_after_instruction_execution()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);
        var serviceRoutineInvoked = new AtomicBoolean(false);
        var serviceRoutineReturned = new AtomicBoolean(false);

        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(!nmiFired.get()) {
                    InterruptSource1.FireNmi();
                    nmiFired.set(true);
                }

                if(Sut.getRegisters().getPC() == 1)
                    serviceRoutineReturned.set(true);

                if(Sut.getRegisters().getPC() == 0x66)
                    serviceRoutineInvoked.set(true);
            });

        Sut.Start(null);

        assertTrue(serviceRoutineInvoked.get());
        assertTrue(serviceRoutineReturned.get());
    }

    @Test
    public void Nmi_resets_IFF1()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args -> {
                if(!nmiFired.get()) {
                    InterruptSource1.FireNmi();
                    nmiFired.set(true);
                }
            });

        Sut.Continue();

        assertTrue(nmiFired.get());
        assertEquals(0, Sut.getRegisters().getIFF1().intValue());
    }

    @ParameterizedTest
    @ValueSource(bytes = {DI_opcode, EI_opcode})
    public void Nmi_is_not_accepted_after_EI_or_DI(byte opcode)
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        Sut.getMemory().set(0, opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(!nmiFired.get()) {
                    InterruptSource1.FireNmi();
                    nmiFired.set(true);
                }

                if(Sut.getRegisters().getPC() == 0x66)
                    serviceRoutineInvoked.set(true);

                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.Continue();

        assertTrue(nmiFired.get());
        assertFalse(serviceRoutineInvoked.get());
    }

//#endregion

//#region Accepting INT interrupts

    @Test
    public void Int_acceptance_clears_IFF1_and_IFF2()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        InterruptSource1.setIntLineIsActive(true);
        InterruptSource1.setValueOnDataBus(RST20h_opcode);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.Continue();

        assertEquals(0, Sut.getRegisters().getIFF1().intValue());
        assertEquals(0, Sut.getRegisters().getIFF2().intValue());
    }

    @Test
    public void Int_is_not_accepted_with_ints_disabled()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        InterruptSource1.setIntLineIsActive(true);
        InterruptSource1.setValueOnDataBus(RST20h_opcode);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x20, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.OFF);
        Sut.getRegisters().setIFF2(Bit.OFF);
        Sut.setInterruptMode((byte) 0);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(Sut.getRegisters().getPC() == 0x20)
                    serviceRoutineInvoked.set(true);

                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.Continue();

        assertFalse(serviceRoutineInvoked.get());
    }

    @Test
    public void Int_executes_opcode_from_data_bus_in_IM0_mode()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        InterruptSource1.setIntLineIsActive(true);
        InterruptSource1.setValueOnDataBus(RST20h_opcode);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x20, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setInterruptMode((byte) 0);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(Sut.getRegisters().getPC() == 0x20)
                    serviceRoutineInvoked.set(true);

                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.Continue();

        assertTrue(serviceRoutineInvoked.get());
    }

    @Test
    public void Int1_executes_RST38h()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        InterruptSource1.setIntLineIsActive(true);
        InterruptSource1.setValueOnDataBus(RST20h_opcode);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(0x38, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setInterruptMode((byte) 1);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(Sut.getRegisters().getPC() == 0x38)
                    serviceRoutineInvoked.set(true);

                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.Continue();

        assertTrue(serviceRoutineInvoked.get());
    }

    @Test
    public void Int2_calls_to_address_composed_from_I_and_data_bus_and_triggers_memory_events()
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        InterruptSource1.setIntLineIsActive(true);

        var registerI = Fixture.create(Byte.TYPE);
        var dataBusValue = Fixture.create(Byte.TYPE);
        var pointerAddress = ToUShort(NumberUtils.createShort(dataBusValue, registerI));
        var calledAddress = Fixture.create(Short.TYPE);
        Sut.getMemory().set(pointerAddress, GetLowByte(calledAddress));
        Sut.getMemory().set(Inc(pointerAddress), GetHighByte(calledAddress));
        Sut.getRegisters().setI(registerI);
        InterruptSource1.setValueOnDataBus(dataBusValue);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, RET_opcode);
        Sut.getMemory().set(calledAddress, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);
        var beforeMemoryReadEventFiredForPointerAddress = new AtomicBoolean(false);
        var afterMemoryReadEventFiredForPointerAddress = new AtomicBoolean(false);

        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setInterruptMode((byte) 2);
        Sut.setAutoStopOnRetWithStackEmpty(true);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(Sut.getRegisters().getPC() == calledAddress)
                    serviceRoutineInvoked.set(true);

                if(Sut.getRegisters().getPC() == 1)
                    args.getExecutionStopper().Stop(false);
            });

        Sut.MemoryAccess().addListener(args ->
            {
                if(args.getAddress() != pointerAddress)
                    return;

                if(args.getEventType() == MemoryAccessEventType.BeforeMemoryRead)
                    beforeMemoryReadEventFiredForPointerAddress.set(true);
                else if(args.getEventType() == MemoryAccessEventType.AfterMemoryRead)
                    afterMemoryReadEventFiredForPointerAddress.set(true);
            });

        Sut.Continue();

        assertTrue(serviceRoutineInvoked.get());
        assertTrue(beforeMemoryReadEventFiredForPointerAddress.get(), "BeforeMemoryRead not fired for pointer");
        assertTrue(afterMemoryReadEventFiredForPointerAddress.get(), "AfterMemoryRead not fired for pointer");
    }

    private void Sut_MemoryAccess(Object sender, MemoryAccessEventArgs e)
    {
        throw new UnsupportedOperationException();
    }

//#endregion

//#region Halt behavior

    @Test
    public void Halt_causes_the_processor_to_execute_NOPs_without_increasing_SP()
    {
        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, HALT_opcode);
        Sut.getMemory().set(2, RET_opcode);
        Sut.getMemory().set(3, RET_opcode);

        Sut.setAutoStopOnDiPlusHalt(false);

        var maxPCreached = new AtomicInteger(0);
        var instructionsExecutedCount = new AtomicInteger(0);
        var nopsExecutedcount = new AtomicInteger(0);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                maxPCreached.set(Math.max(Sut.getRegisters().getPC(), maxPCreached.get()));

                if(instructionsExecutedCount.get() == 5)
                    args.getExecutionStopper().Stop(false);

                instructionsExecutedCount.getAndIncrement();
            });

        Sut.BeforeInstructionExecution().addListener(args ->
            {
                if(args.getOpcode()[0] == 0)
                    nopsExecutedcount.getAndIncrement();
            });

        Sut.Start(null);

        assertTrue(Sut.getIsHalted());
        assertEquals(2, maxPCreached.get());
        assertEquals(4, nopsExecutedcount.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Halted_processor_awakes_on_interrupt(boolean isNmi)
    {
        Sut.RegisterInterruptSource(InterruptSource1);

        Sut.getMemory().set(0, NOP_opcode);
        Sut.getMemory().set(1, HALT_opcode);
        Sut.getMemory().set(2, RET_opcode);
        Sut.getMemory().set(3, RET_opcode);
        Sut.getMemory().set(0x66, RET_opcode);
        Sut.getMemory().set(0x38, RET_opcode);

        Sut.setAutoStopOnDiPlusHalt(false);
        Sut.setAutoStopOnRetWithStackEmpty(true);
        Sut.Reset();
        Sut.getRegisters().setIFF1(Bit.ON);
        Sut.setInterruptMode((byte) 1);

        var instructionsExecutedCount = new AtomicInteger(0);

        Sut.BeforeInstructionFetch().addListener(args ->
            {
                if(instructionsExecutedCount.get() == 10)
                    if(isNmi)
                        InterruptSource1.FireNmi();
                    else
                        InterruptSource1.setIntLineIsActive(true);

                if(instructionsExecutedCount.get() == 15)
                    args.getExecutionStopper().Stop(false);
                else
                    instructionsExecutedCount.getAndIncrement();
            });

        Sut.Continue();

        assertFalse(Sut.getIsHalted());
        assertEquals(13, instructionsExecutedCount.get()); //10 + extra NOP + RET on 0x66 + RET on 2
        assertEquals(StopReason.RetWithStackEmpty, Sut.getStopReason());
    }

//#endregion
}
