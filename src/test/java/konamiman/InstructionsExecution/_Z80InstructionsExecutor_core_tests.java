package konamiman.InstructionsExecution;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class Z80InstructionsExecutor_core_test extends InstructionsExecutionTestsBase {

    private static final byte NOP_opcode = 0x00;
    private static final byte LD_BC_nn_opcode = 0x01;
    private static final byte ADD_HL_BC_opcode = 0x09;
    private static final byte IN_B_C_opcode = 0x40;
    private static final byte RLC_B_opcode = 0x00;

    @Test
    public void InstructionsExecution_fire_FetchFinished_event_and_return_proper_T_states_count() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        Sut.InstructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        SetMemoryContents((byte) 0);
        assertEquals(4, Execute(NOP_opcode, null));
        SetMemoryContents((byte) 0, Fixture.create(Byte.TYPE), Fixture.create(Byte.TYPE));
        assertEquals(10, Execute(LD_BC_nn_opcode, null));

        SetMemoryContents((byte) 0);
        assertEquals(8, Execute((byte) 0xCB, null, /*nextFetches:*/ (byte) 0));

        assertEquals(15, Execute(ADD_HL_BC_opcode, null, (byte) 0xDD));
        assertEquals(15, Execute(ADD_HL_BC_opcode, null, (byte) 0xFD));

        assertEquals(12, Execute(IN_B_C_opcode, null, (byte) 0xED));

        assertEquals(23, Execute((byte) 0xCB, null, (byte) 0xDD), (byte) 0);
        assertEquals(23, Execute((byte) 0xCB, null, (byte) 0xFD), (byte) 0);

        assertEquals(8, fetchFinishedEventsCount.get());
    }

    @Test
    public void Unsupported_instructions_just_return_8_TStates_elapsed() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        Sut.InstructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        assertEquals(8, Execute((byte) 0x3F, null, (byte) 0xED));
        assertEquals(8, Execute((byte) 0xC0, null, (byte) 0xED));
        assertEquals(8, Execute((byte) 0x80, null, (byte) 0xED));
        assertEquals(8, Execute((byte) 0x9F, null, (byte) 0xED));

        assertEquals(4, fetchFinishedEventsCount.get());
    }

    @Test
    public void Unsupported_instructions_invoke_overridable_method_ExecuteUnsopported_ED_Instruction() {
        Sut = NewFakeInstructionExecutor();

        Execute((byte) 0x3F, null, (byte) 0xED);
        Execute((byte) 0xC0, null, (byte) 0xED);

        assertArrayEquals(new Byte[] {0x3F, (byte) 0xC0}, ((FakeInstructionExecutor) Sut).UnsupportedExecuted.toArray());
    }

    @Test
    public void Execute_increases_R_appropriately() {
        Registers.setR((byte) 0xFE);

        Execute(NOP_opcode, null);
        assertEquals(0xFF, Registers.getR());

        Execute(LD_BC_nn_opcode, null, Fixture.create(Byte.TYPE), Fixture.create(Byte.TYPE));
        assertEquals(0x80, Registers.getR());

        Execute(RLC_B_opcode, null, (byte) 0xCB);
        assertEquals(0x82, Registers.getR());

        Execute(ADD_HL_BC_opcode, null, (byte) 0xDD);
        assertEquals(0x84, Registers.getR());

        Execute(ADD_HL_BC_opcode, null, (byte) 0xFD);
        assertEquals(0x86, Registers.getR());

        Execute(IN_B_C_opcode, null, (byte) 0xED);
        assertEquals(0x88, Registers.getR());

        Execute((byte) 0xCB, null, (byte) 0xDD, (byte) 0);
        assertEquals(0x8A, Registers.getR());

        Execute((byte) 0xCB, null, (byte) 0xFD, (byte) 0);
        assertEquals(0x8C, Registers.getR());
    }

    @Test
    public void DD_FD_not_followed_by_valid_opcode_are_trated_as_nops() {
        var fetchFinishedEventsCount = new AtomicInteger(0);

        Sut.InstructionFetchFinished().addListener(e -> fetchFinishedEventsCount.incrementAndGet());

        assertEquals(4, Execute((byte) 0xFD, null, (byte) 0xDD));
        assertEquals(4, Execute((byte) 0x01, null, (byte) 0xFD));
        assertEquals(10, Execute((byte) 0x01, null, Fixture.create(Byte.TYPE), Fixture.create(Byte.TYPE)));

        assertEquals(3, fetchFinishedEventsCount.get());
    }
}
