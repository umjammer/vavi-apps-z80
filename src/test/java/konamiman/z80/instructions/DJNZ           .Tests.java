package konamiman.z80.instructions;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.dec;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DJNZ_tests extends InstructionsExecutionTestsBase {

    private static final byte DJNZ_opcode = 0x10;

    @Test
    public void DJNZ_decreases_B() {
        var value = fixture.create(Byte.TYPE);
        registers.setB(value);

        execute(DJNZ_opcode, null);

        assertEquals(dec(value), registers.getB());
    }

    @Test
    public void DJNZ_does_not_jump_if_B_decreases_to_zero() {
        var instructionAddress = fixture.create(Short.TYPE);

        registers.setB((byte) 1);
        executeAt(instructionAddress, DJNZ_opcode, null, fixture.create(Byte.TYPE));

        assertEquals(instructionAddress + 2, registers.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_no_jump_is_done() {
        registers.setB((byte) 1);
        var states = execute(DJNZ_opcode, null);

        assertEquals(8, states);
    }

    @Test
    public void DJNZ_jumps_to_proper_address_if_B_does_not_decrease_to_zero() {
        var instructionAddress = fixture.create(Short.TYPE);

        registers.setB((byte) 0);
        executeAt(instructionAddress, DJNZ_opcode, null, (byte) 0x7F);
        assertEquals(NumberUtils.add(instructionAddress,129), registers.getPC());

        registers.setB((byte) 0);
        executeAt(instructionAddress, DJNZ_opcode, null, (byte) 0x80);
        assertEquals(NumberUtils.sub(instructionAddress, 126), registers.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_jump_is_done() {
        registers.setB((byte) 0);
        var states = execute(DJNZ_opcode, null);

        assertEquals(13, states);
    }

    @Test
    public void DJNZ_does_not_modify_flags() {
        assertNoFlagsAreModified(DJNZ_opcode, null);
    }
}