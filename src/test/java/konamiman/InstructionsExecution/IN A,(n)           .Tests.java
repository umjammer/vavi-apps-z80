package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class IN_A_n_tests extends InstructionsExecutionTestsBase {

    private static final byte IN_A_n_opcode = (byte) 0xDB;

    @Test
    public void IN_A_n_reads_value_from_port() {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);

        Registers.setA(oldValue);
        SetPortValue(portNumber, value);

        Execute(IN_A_n_opcode, null, portNumber);

        assertEquals(value, Registers.getA());
    }

    @Test
    public void IN_A_n_does_not_modify_flags() {
        AssertDoesNotChangeFlags(IN_A_n_opcode, null);
    }

    @Test
    public void IN_A_n_returns_proper_T_states() {
        var states = Execute(IN_A_n_opcode, null);
        assertEquals(11, states);
    }
}
