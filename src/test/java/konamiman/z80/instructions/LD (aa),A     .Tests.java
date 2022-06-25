package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_aa_A_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_aa_A_opcode = 0x32;

    @Test
    public void LD_aa_A_loads_value_in_memory() {
        var address = fixture.create(Short.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        registers.setA(newValue);
        processorAgent.writeToMemory(address, oldValue);

        execute(LD_aa_A_opcode, null, getLowByte(address), getHighByte(address));

        assertEquals(newValue, processorAgent.readFromMemory(address));
    }

    @Test
    public void LD_aa_A_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_aa_A_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        var states = execute(LD_aa_A_opcode, null);
        assertEquals(13, states);
    }
}
