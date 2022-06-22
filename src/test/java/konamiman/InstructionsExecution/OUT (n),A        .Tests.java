package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class OUT_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte OUT_A_n_opcode = (byte) 0xD3;

    @Test
    public void OUT_A_n_reads_value_from_port() {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);

        Registers.setA(value);
        SetPortValue(portNumber, oldValue);

        Execute(OUT_A_n_opcode, null, portNumber);

        assertEquals(value, GetPortValue(portNumber));
    }

    @Test
    public void OUT_A_n_does_not_modify_flags() {
        AssertDoesNotChangeFlags(OUT_A_n_opcode, null);
    }

    @Test
    public void OUT_A_n_returns_proper_T_states() {
        var states = Execute(OUT_A_n_opcode, null);
        assertEquals(11, states);
    }
}
