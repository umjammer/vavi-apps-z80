package konamiman.z80.instructions;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CCF_tests extends InstructionsExecutionTestsBase {

    private static final byte CCF_opcode = 0x3F;

    @Test
    void CCF_complements_CF_correctly() {
        registers.setCF(Bit.OFF);
        execute(CCF_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        registers.setCF(Bit.ON);
        execute(CCF_opcode, null);
        assertEquals(0, registers.getCF().intValue());
    }

    @Test
    void CCF_sets_H_as_previous_carry() {
        registers.setCF(Bit.OFF);
        registers.setHF(Bit.ON);
        execute(CCF_opcode, null);
        assertEquals(0, registers.getHF().intValue());

        registers.setCF(Bit.ON);
        registers.setHF(Bit.OFF);
        execute(CCF_opcode, null);
        assertEquals(1, registers.getHF().intValue());
    }

    @Test
    void CCF_resets_N() {
        assertResetsFlags(CCF_opcode, null, "N");
    }

    @Test
    void CCF_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(CCF_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    void CCF_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(CCF_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    void CCF_returns_proper_T_states() {
        var states = execute(CCF_opcode, null);
        assertEquals(4, states);
    }
}
