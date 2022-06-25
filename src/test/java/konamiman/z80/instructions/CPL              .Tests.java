package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CPL_tests extends InstructionsExecutionTestsBase {

    private static final byte CPL_opcode = 0x2F;

    @Test
    public void CPL_complements_byte_correctly() {
        var value = fixture.create(Byte.TYPE);
        registers.setA(value);

        execute(CPL_opcode, null);

        assertEquals((byte) (~value), registers.getA());
    }

    @Test
    public void CPL_sets_H_and_N() {
        assertSetsFlags(CPL_opcode, null, "H", "N");
    }

    @Test
    public void CPL_does_not_change_SF_ZF_PF_CF() {
        assertDoesNotChangeFlags(CPL_opcode, null, "S", "Z", "P", "C");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void CPL_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(CPL_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void CPL_returns_proper_T_states() {
        var states = execute(CPL_opcode, null);
        assertEquals(4, states);
    }
}
