package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aHL_n_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aHL_n_opcode = 0x36;

    @Test
    public void LD_aHL_n_loads_value_in_memory() {
        var address = fixture.create(Short.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(address, oldValue);
        registers.setHL(address);

        execute(LD_aHL_n_opcode, null, newValue);

        assertEquals(newValue, processorAgent.readFromMemory(address));
    }

    @Test
    public void LD_aHL_n_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_aHL_n_opcode, null);
    }

    @Test
    public void LD_aHL_n_returns_proper_T_states() {
        var states = execute(LD_aHL_n_opcode, null);
        assertEquals(10, states);
    }
}
