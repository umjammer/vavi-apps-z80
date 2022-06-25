package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;

import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LD_A_aa_tests extends InstructionsExecutionTestsBase {

    private static final byte LD_A_aa_opcode = 0x3A;

    @Test
    public void LD_A_aa_loads_value_from_memory() {
        // TODO got error when 1
        var address = createAddressFixture();
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        registers.setA(oldValue);
        processorAgent.writeToMemory(address, newValue);

        execute(LD_A_aa_opcode, null, getLowByte(address), getHighByte(address));

        assertEquals(newValue, registers.getA());
    }

    @Test
    public void LD_A_aa_does_not_modify_flags() {
        assertNoFlagsAreModified(LD_A_aa_opcode, null);
    }

    @Test
    public void LD_rr_r_returns_proper_T_states() {
        var states = execute(LD_A_aa_opcode, null);
        assertEquals(13, states);
    }
}
