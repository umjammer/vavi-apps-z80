package konamiman.z80.instructions;

import konamiman.z80.utils.NumberUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class OUT_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte OUT_A_n_opcode = (byte) 0xD3;

    @Test
    public void OUT_A_n_writes_value_to_port() {
        var portNumberLow = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);

        registers.setA(value);
        setPortValue(NumberUtils.createShort(portNumberLow, value), oldValue);

        execute(OUT_A_n_opcode, null, portNumberLow);

        assertEquals(value, getPortValue(NumberUtils.createShort(portNumberLow, value)));
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
