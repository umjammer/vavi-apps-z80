package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aHL_n_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aHL_n_opcode = 0x36;

    @Test
    public void LD_aHL_n_loads_value_in_memory() {
        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[address] = oldValue;
        Registers.setHL(ToShort(address));

        Execute(LD_aHL_n_opcode, null, newValue);

        assertEquals(newValue, ProcessorAgent.getMemory()[address]);
    }

    @Test
    public void LD_aHL_n_does_not_modify_flags() {
        AssertNoFlagsAreModified(LD_aHL_n_opcode, null);
    }

    @Test
    public void LD_aHL_n_returns_proper_T_states() {
        var states = Execute(LD_aHL_n_opcode, null);
        assertEquals(10, states);
    }
}
