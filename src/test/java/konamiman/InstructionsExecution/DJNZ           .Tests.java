package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DJNZ_tests extends InstructionsExecutionTestsBase {

    private static final byte DJNZ_opcode = 0x10;

    @Test
    public void DJNZ_decreases_B() {
        var value = Fixture.create(Byte.TYPE);
        Registers.setB(value);

        Execute(DJNZ_opcode, null);

        assertEquals(Dec(value), Registers.getB());
    }

    @Test
    public void DJNZ_does_not_jump_if_B_decreases_to_zero() {
        var instructionAddress = Fixture.create(Short.TYPE);

        Registers.setB((byte) 1);
        ExecuteAt(instructionAddress, DJNZ_opcode, null, /*nextFetches:*/ Fixture.create(Byte.TYPE));

        assertEquals(instructionAddress + 2, Registers.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_no_jump_is_done() {
        Registers.setB((byte) 1);
        var states = Execute(DJNZ_opcode, null);

        assertEquals(8, states);
    }

    @Test
    public void DJNZ_jumps_to_proper_address_if_B_does_not_decrease_to_zero() {
        var instructionAddress = Fixture.create(Short.TYPE);

        Registers.setB((byte) 0);
        ExecuteAt(instructionAddress, DJNZ_opcode, null, /*nextFetches:*/ (byte) 0x7F);
        assertEquals(Add(instructionAddress, (short) 129), Registers.getPC());

        Registers.setB((byte) 0);
        ExecuteAt(instructionAddress, DJNZ_opcode, null, /*nextFetches:*/ (byte) 0x80);
        assertEquals(Sub(instructionAddress, (short) 126), Registers.getPC());
    }

    @Test
    public void DJNZ_returns_proper_T_states_when_jump_is_done() {
        Registers.setB((byte) 0);
        var states = Execute(DJNZ_opcode, null);

        assertEquals(13, states);
    }

    @Test
    public void DJNZ_does_not_modify_flags() {
        AssertNoFlagsAreModified(DJNZ_opcode, null);
    }
}