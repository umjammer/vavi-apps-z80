package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class OUT_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte OUT_A_n_opcode = (byte) 0xD3;

    @Test
    public void OUT_A_n_reads_value_from_port() {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);

        registers.setA(value);
        setPortValue(portNumber, oldValue);

        execute(OUT_A_n_opcode, null, portNumber);

        assertEquals(value, getPortValue(portNumber));
    }

    @Test
    public void OUT_A_n_does_not_modify_flags() {
        assertDoesNotChangeFlags(OUT_A_n_opcode, null);
    }

    @Test
    public void OUT_A_n_returns_proper_T_states() {
        var states = execute(OUT_A_n_opcode, null);
        assertEquals(11, states);
    }
}
