package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_A_aa_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_A_aa_opcode = 0x3A;

    @Test
    public void LD_A_aa_loads_value_from_memory() {
        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);

        Registers.setA(oldValue);
        ProcessorAgent.getMemory()[address] = newValue;

        Execute(LD_A_aa_opcode, null, /*nextFetches:*/ GetLowByte(address), GetHighByte(address));

        assertEquals(newValue, Registers.getA());
    }

    @Test
    public void LD_A_aa_does_not_modify_flags() {
        AssertNoFlagsAreModified(LD_A_aa_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        var states = Execute(LD_A_aa_opcode, null);
        assertEquals(13, states);
    }
}
