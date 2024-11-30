package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class Z80InstructionsExecutor_core_test extends InstructionsExecutionTestsBase {

    private static final byte NOP_opcode = 0x00;
    private static final byte LD_BC_nn_opcode = 0x01;
    private static final byte ADD_HL_BC_opcode = 0x09;
    private static final byte IN_B_C_opcode = 0x40;
    private static final byte RLC_B_opcode = 0x00;

    @Test
    public void InstructionsExecution_fire_FetchFinished_event_and_return_proper_T_states_count() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        setMemoryContents((byte) 0);
        assertEquals(4, execute(NOP_opcode, null));
        setMemoryContents((byte) 0, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE));
        assertEquals(10, execute(LD_BC_nn_opcode, null));

        setMemoryContents((byte) 0);
        assertEquals(8, execute((byte) 0xCB, null, (byte) 0));

        assertEquals(15, execute(ADD_HL_BC_opcode, (byte) 0xDD));
        assertEquals(15, execute(ADD_HL_BC_opcode, (byte) 0xFD));

        assertEquals(12, execute(IN_B_C_opcode, (byte) 0xED));

        assertEquals(23, execute((byte) 0xCB, (byte) 0xDD), (byte) 0);
        assertEquals(23, execute((byte) 0xCB, (byte) 0xFD), (byte) 0);

        assertEquals(8, fetchFinishedEventsCount.get());
    }

    @Test
    public void Unsupported_instructions_just_return_8_TStates_elapsed() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        assertEquals(8, execute((byte) 0x3F, (byte) 0xED));
        assertEquals(8, execute((byte) 0xC0, (byte) 0xED));
        assertEquals(8, execute((byte) 0x80, (byte) 0xED));
        assertEquals(8, execute((byte) 0x9F, (byte) 0xED));

        assertEquals(4, fetchFinishedEventsCount.get());
    }

    @Test
    public void Unsupported_instructions_invoke_overridable_method_ExecuteUnsupported_ED_Instruction() {
        sut = newFakeInstructionExecutor();

        execute((byte) 0x3F, (byte) 0xED);
        execute((byte) 0xC0, (byte) 0xED);

        assertArrayEquals(new Byte[] {0x3F, (byte) 0xC0}, ((FakeInstructionExecutor) sut).unsupportedExecuted.toArray());
    }

    @Test
    public void Execute_increases_R_appropriately() {
        registers.setR((byte) 0xFE);

        execute(NOP_opcode, null);
        assertEquals((byte) 0xFF, registers.getR());

        execute(LD_BC_nn_opcode, null, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE));
        assertEquals((byte) 0x80, registers.getR());

        execute(RLC_B_opcode, (byte) 0xCB);
        assertEquals((byte) 0x82, registers.getR());

        execute(ADD_HL_BC_opcode, (byte) 0xDD);
        assertEquals((byte) 0x84, registers.getR());

        execute(ADD_HL_BC_opcode, (byte) 0xFD);
        assertEquals((byte) 0x86, registers.getR());

        execute(IN_B_C_opcode, (byte) 0xED);
        assertEquals((byte) 0x88, registers.getR());

        execute((byte) 0xCB, null, (byte) 0xDD, (byte) 0);
        assertEquals((byte) 0x8A, registers.getR());

        execute((byte) 0xCB, null, (byte) 0xFD, (byte) 0);
        assertEquals((byte) 0x8C, registers.getR());
    }

    @Test
    public void DD_FD_not_followed_by_valid_opcode_are_treated_as_nops() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        sut.instructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        assertEquals(4, execute((byte) 0xFD, (byte) 0xDD));
        assertEquals(4, execute((byte) 0x01, (byte) 0xFD));
        assertEquals(10, execute((byte) 0x01, null, fixture.create(Byte.TYPE), fixture.create(Byte.TYPE)));

        assertEquals(3, fetchFinishedEventsCount.get());
    }

    @Test
    void testAddressFixture() {
        assertNotEquals((short) 1, createAddressFixture((short) 1));
        assertNotEquals((short) 0, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertNotEquals((short) 1, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertNotEquals((short) 2, createAddressFixture((short) 0, (short) 1, (short) 2));
        assertTrue(3 != createAddressFixture((short) 0, (short) 1, (short) 2, (short) 3));
        assertNotEquals((short) 1, createAddressFixture());
    }
}
