package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CPL_tests extends InstructionsExecutionTestsBase {

    private static final byte CPL_opcode = 0x2F;

    @Test
    public void CPL_complements_byte_correctly() {
        var value = Fixture.create(Byte.TYPE);
        Registers.setA(value);

        Execute(CPL_opcode, null);

        assertEquals((byte) (~value), Registers.getA());
    }

    @Test
    public void CPL_sets_H_and_N() {
        AssertSetsFlags(CPL_opcode, null, "H", "N");
    }

    @Test
    public void CPL_does_not_change_SF_ZF_PF_CF() {
        AssertDoesNotChangeFlags(CPL_opcode, null, "S", "Z", "P", "C");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void CPL_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(CPL_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    public void CPL_returns_proper_T_states() {
        var states = Execute(CPL_opcode, null);
        assertEquals(4, states);
    }
}
