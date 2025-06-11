package konamiman.z80;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.flextrade.jfixture.JFixture;
import konamiman.z80.enums.MemoryAccessEventType;
import konamiman.z80.enums.StopReason;
import konamiman.z80.events.MemoryAccessEvent;
import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class Z80ProcessorTests_Interrupts {

    private static final byte RET_opcode = (byte) 0xc9;
    private static final byte DI_opcode = (byte) 0xf3;
    private static final byte EI_opcode = (byte) 0xfb;
    private static final byte HALT_opcode = 0x76;
    private static final byte NOP_opcode = 0x00;
    private static final byte RST20h_opcode = (byte) 0xe7;
    private static final byte IM0_opcode = 0x46;
    private static final byte IM1_opcode = 0x56;
    private static final byte IM2_opcode = 0x5e;

    Z80ProcessorForTests sut;
    JFixture fixture;
    InterruptSourceForTests interruptSource1;
    InterruptSourceForTests interruptSource2;

    @BeforeEach
    public void setup() {
        fixture = new JFixture();

        sut = new Z80ProcessorForTests();
        sut.setInstructionExecutionContextToNonNull();

        interruptSource1 = new InterruptSourceForTests();
        interruptSource2 = new InterruptSourceForTests();
    }

    @Test
    public void Can_create_instances() {
        assertNotNull(sut);
    }

//#region Interrupt source registration

    @Test
    public void RegisterInterruptSource_and_GetRegisteredSources_are_symmetrical() {
        sut.registerInterruptSource(interruptSource1);
        sut.registerInterruptSource(interruptSource2);

        var expected = new Object[] {interruptSource1, interruptSource2};
        var actual = sut.getRegisteredInterruptSources();
        assertArrayEquals(expected, actual.toArray());
    }

    @Test
    public void RegisterInterruptSource_does_not_register_same_instance_twice() {
        sut.registerInterruptSource(interruptSource1);
        sut.registerInterruptSource(interruptSource1);

        var expected = new Object[] {interruptSource1};
        var actual = sut.getRegisteredInterruptSources();
        assertArrayEquals(expected, actual.toArray());
    }

    @Test
    public void UnregisterAllInterruptSources_clears_sources_list() {
        sut.registerInterruptSource(interruptSource1);
        sut.registerInterruptSource(interruptSource2);

        sut.unregisterAllInterruptSources();

        var actual = sut.getRegisteredInterruptSources();
        assertTrue(actual.isEmpty());
    }

//#endregion

//#region Accepting NMI interrupts

    @Test
    public void Nmi_is_accepted_after_instruction_execution() {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);
        var serviceRoutineInvoked = new AtomicBoolean(false);
        var serviceRoutineReturned = new AtomicBoolean(false);

        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (!nmiFired.get()) {
                interruptSource1.fireNmi();
                nmiFired.set(true);
            }

            if (sut.getRegisters().getPC() == 1)
                serviceRoutineReturned.set(true);

            if (sut.getRegisters().getPC() == 0x66)
                serviceRoutineInvoked.set(true);
        });

        sut.start(null);

        assertTrue(serviceRoutineInvoked.get());
        assertTrue(serviceRoutineReturned.get());
    }

    @Test
    public void Nmi_resets_IFF1() {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (!nmiFired.get()) {
                interruptSource1.fireNmi();
                nmiFired.set(true);
            }
        });

        sut.continue_();

        assertTrue(nmiFired.get());
        assertEquals(0, sut.getRegisters().getIFF1().intValue());
    }

    @ParameterizedTest
    @ValueSource(bytes = {DI_opcode, EI_opcode})
    public void Nmi_is_not_accepted_after_EI_or_DI(byte opcode) {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x66, RET_opcode);
        var nmiFired = new AtomicBoolean(false);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (!nmiFired.get()) {
                interruptSource1.fireNmi();
                nmiFired.set(true);
            }

            if (sut.getRegisters().getPC() == 0x66)
                serviceRoutineInvoked.set(true);

            if (sut.getRegisters().getPC() == 1)
                args.getExecutionStopper().stop(false);
        });

        sut.continue_();

        assertTrue(nmiFired.get());
        assertFalse(serviceRoutineInvoked.get());
    }

//#endregion

//#region Accepting INT interrupts

    @Test
    public void Int_acceptance_clears_IFF1_and_IFF2() {
        sut.registerInterruptSource(interruptSource1);

        interruptSource1.setIntLineIsActive(true);
        interruptSource1.setValueOnDataBus(RST20h_opcode);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (sut.getRegisters().getPC() == 1) {
                args.getExecutionStopper().stop(false);
            }
        });

        sut.continue_();

        assertEquals(0, sut.getRegisters().getIFF1().intValue());
        assertEquals(0, sut.getRegisters().getIFF2().intValue());
    }

    @Test
    public void Int_is_not_accepted_with_ints_disabled() {
        sut.registerInterruptSource(interruptSource1);

        interruptSource1.setIntLineIsActive(true);
        interruptSource1.setValueOnDataBus(RST20h_opcode);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x20, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.OFF);
        sut.getRegisters().setIFF2(Bit.OFF);
        sut.setInterruptMode((byte) 0);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (sut.getRegisters().getPC() == 0x20)
                serviceRoutineInvoked.set(true);

            if (sut.getRegisters().getPC() == 1)
                args.getExecutionStopper().stop(false);
        });

        sut.continue_();

        assertFalse(serviceRoutineInvoked.get());
    }

    @Test
    public void Int_executes_opcode_from_data_bus_in_IM0_mode() {
        sut.registerInterruptSource(interruptSource1);

        interruptSource1.setIntLineIsActive(true);
        interruptSource1.setValueOnDataBus(RST20h_opcode);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x20, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setInterruptMode((byte) 0);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (sut.getRegisters().getPC() == 0x20)
                serviceRoutineInvoked.set(true);

            if (sut.getRegisters().getPC() == 1)
                args.getExecutionStopper().stop(false);
        });

        sut.continue_();

        assertTrue(serviceRoutineInvoked.get());
    }

    @Test
    public void Int1_executes_RST38h() {
        sut.registerInterruptSource(interruptSource1);

        interruptSource1.setIntLineIsActive(true);
        interruptSource1.setValueOnDataBus(RST20h_opcode);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(0x38, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setInterruptMode((byte) 1);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (sut.getRegisters().getPC() == 0x38)
                serviceRoutineInvoked.set(true);

            if (sut.getRegisters().getPC() == 1)
                args.getExecutionStopper().stop(false);
        });

        sut.continue_();

        assertTrue(serviceRoutineInvoked.get());
    }

    @Test
    public void Int2_calls_to_address_composed_from_I_and_data_bus_and_triggers_memory_events() {
        sut.registerInterruptSource(interruptSource1);

        interruptSource1.setIntLineIsActive(true);

        var registerI = fixture.create(Byte.TYPE);
        var dataBusValue = fixture.create(Byte.TYPE);
        var pointerAddress = createShort(dataBusValue, registerI);
        var calledAddress = fixture.create(Short.TYPE);
        sut.getMemory().set(pointerAddress & 0xffff, getLowByte(calledAddress));
        sut.getMemory().set(NumberUtils.inc(pointerAddress), getHighByte(calledAddress));
        sut.getRegisters().setI(registerI);
        interruptSource1.setValueOnDataBus(dataBusValue);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, RET_opcode);
        sut.getMemory().set(calledAddress & 0xffff, RET_opcode);
        var serviceRoutineInvoked = new AtomicBoolean(false);
        var beforeMemoryReadEventFiredForPointerAddress = new AtomicBoolean(false);
        var afterMemoryReadEventFiredForPointerAddress = new AtomicBoolean(false);

        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setInterruptMode((byte) 2);
        sut.setAutoStopOnRetWithStackEmpty(true);

        sut.beforeInstructionFetch().addListener(args -> {
            if (sut.getRegisters().getPC() == calledAddress)
                serviceRoutineInvoked.set(true);

            if (sut.getRegisters().getPC() == 1)
                args.getExecutionStopper().stop(false);
        });

        sut.memoryAccess().addListener(args -> {
            if (args.getAddress() != pointerAddress)
                return;

            if (args.getEventType() == MemoryAccessEventType.BeforeMemoryRead)
                beforeMemoryReadEventFiredForPointerAddress.set(true);
            else if (args.getEventType() == MemoryAccessEventType.AfterMemoryRead)
                afterMemoryReadEventFiredForPointerAddress.set(true);
        });

        sut.continue_();

        assertTrue(serviceRoutineInvoked.get());
        assertTrue(beforeMemoryReadEventFiredForPointerAddress.get(), "BeforeMemoryRead not fired for pointer");
        assertTrue(afterMemoryReadEventFiredForPointerAddress.get(), "AfterMemoryRead not fired for pointer");
    }

    private static void Sut_MemoryAccess(Object sender, MemoryAccessEvent e) {
        throw new UnsupportedOperationException();
    }

//#endregion

//#region Halt behavior

    @Test
    public void Halt_causes_the_processor_to_execute_NOPs_without_increasing_SP() {
        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, HALT_opcode);
        sut.getMemory().set(2, RET_opcode);
        sut.getMemory().set(3, RET_opcode);

        sut.setAutoStopOnDiPlusHalt(false);

        var maxPCreached = new AtomicInteger(0);
        var instructionsExecutedCount = new AtomicInteger(0);
        var nopsExecutedCount = new AtomicInteger(0);

        sut.beforeInstructionFetch().addListener(args -> {
            maxPCreached.set(Math.max(sut.getRegisters().getPC(), maxPCreached.get()));

            if (instructionsExecutedCount.get() == 5)
                args.getExecutionStopper().stop(false);

            instructionsExecutedCount.getAndIncrement();
        });

        sut.beforeInstructionExecution().addListener(args -> {
            if (args.getOpcode()[0] == 0)
                nopsExecutedCount.getAndIncrement();
        });

        sut.start(null);

        assertTrue(sut.isHalted());
        assertEquals(2, maxPCreached.get());
        assertEquals(4, nopsExecutedCount.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void Halted_processor_awakes_on_interrupt(boolean isNmi) {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, NOP_opcode);
        sut.getMemory().set(1, HALT_opcode);
        sut.getMemory().set(2, RET_opcode);
        sut.getMemory().set(3, RET_opcode);
        sut.getMemory().set(0x66, RET_opcode);
        sut.getMemory().set(0x38, RET_opcode);

        sut.setAutoStopOnDiPlusHalt(false);
        sut.setAutoStopOnRetWithStackEmpty(true);
        sut.reset();
        sut.getRegisters().setIFF1(Bit.ON);
        sut.setInterruptMode((byte) 1);

        var instructionsExecutedCount = new AtomicInteger(0);

        sut.beforeInstructionFetch().addListener(args -> {
            if (instructionsExecutedCount.get() == 10)
                if (isNmi)
                    interruptSource1.fireNmi();
                else
                    interruptSource1.setIntLineIsActive(true);

            if (instructionsExecutedCount.get() == 15) {
                args.getExecutionStopper().stop(false);
            } else
                instructionsExecutedCount.getAndIncrement();
        });

        sut.continue_();

        assertFalse(sut.isHalted());
        assertEquals(13, instructionsExecutedCount.get()); // 10 + extra NOP + RET on 0x66 + RET on 2
        assertEquals(StopReason.RetWithStackEmpty, sut.getStopReason());

        sut.unregisterAllInterruptSources();
    }

//#endregion

//#region Interrupt servicing start events

    @Test
    public void Fires_NonMaskableInterruptServicingStart() {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, NOP_opcode);

        var nonMaskableInterruptServicingStartFired = new AtomicBoolean(false);
        sut.nonMaskableInterruptServicingStart().addListener(e -> {
            nonMaskableInterruptServicingStartFired.set(true);
        });

        interruptSource1.fireNmi();

        sut.executeNextInstruction();

        assertTrue(nonMaskableInterruptServicingStartFired.get());
    }

    @ParameterizedTest
    @ValueSource(bytes = {IM0_opcode, IM1_opcode, IM2_opcode})
    public void Fires_MaskableInterruptServicingStart(byte imOpcode) {
        sut.registerInterruptSource(interruptSource1);

        sut.getMemory().set(0, (byte) 0xed);
        sut.getMemory().set(1, imOpcode);
        sut.getMemory().set(2, EI_opcode);
        sut.getMemory().set(3, NOP_opcode);

        sut.executeNextInstruction(); // This sets interrupt mode
        sut.executeNextInstruction(); // This runs EI

        var maskableInterruptServicingStartFired = new AtomicBoolean(false);
        sut.maskableInterruptServicingStart().addListener(e -> {
            maskableInterruptServicingStartFired.set(true);
        });

        interruptSource1.setIntLineIsActive(true);

        sut.executeNextInstruction();

        assertTrue(maskableInterruptServicingStartFired.get());
    }

//#endregion
}
