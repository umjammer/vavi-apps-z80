package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aa_A_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aa_A_opcode = 0x32;

    @Test
    public void LD_aa_A_loads_value_in_memory() {
        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);

        Registers.setA(newValue);
        ProcessorAgent.getMemory()[address] = oldValue;

        Execute(LD_aa_A_opcode, null, /*nextFetches:*/ GetLowByte(address), GetHighByte(address));

        assertEquals(newValue, ProcessorAgent.getMemory()[address]);
    }

    @Test
    public void LD_aa_A_does_not_modify_flags() {
        AssertNoFlagsAreModified(LD_aa_A_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        var states = Execute(LD_aa_A_opcode, null);
        assertEquals(13, states);
    }
}
