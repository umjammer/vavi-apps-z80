package konamiman.z80.instructions;

import konamiman.z80.utils.NumberUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class IN_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte IN_A_n_opcode = (byte) 0xDB;

    @Test
    public void IN_A_n_reads_value_from_port() {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);

        registers.setA(oldValue);
        setPortValue(NumberUtils.createShort(portNumber, oldValue), value);

        execute(IN_A_n_opcode, null, portNumber);

        assertEquals(value, registers.getA());
    }

    @Test
    public void IN_A_n_does_not_modify_flags() {
        assertDoesNotChangeFlags(IN_A_n_opcode, null);
    }

    @Test
    public void IN_A_n_returns_proper_T_states() {
        var states = execute(IN_A_n_opcode, null);
        assertEquals(11, states);
    }
}
