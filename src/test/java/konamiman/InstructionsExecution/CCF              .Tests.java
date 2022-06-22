package konamiman.InstructionsExecution;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CCF_tests extends InstructionsExecutionTestsBase {
    private static final byte CCF_opcode = 0x3F;

    @Test
    void CCF_complements_CF_correctly() {
        Registers.setCF(Bit.OFF);
        Execute(CCF_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Registers.setCF(Bit.ON);
        Execute(CCF_opcode, null);
        assertEquals(0, Registers.getCF().intValue());
    }

    @Test
    void CCF_sets_H_as_previous_carry() {
        Registers.setCF(Bit.OFF);
        Registers.setHF(Bit.ON);
        Execute(CCF_opcode, null);
        assertEquals(0, Registers.getHF().intValue());

        Registers.setCF(Bit.ON);
        Registers.setHF(Bit.OFF);
        Execute(CCF_opcode, null);
        assertEquals(1, Registers.getHF().intValue());
    }

    @Test
    void CCF_resets_N() {
        AssertResetsFlags(CCF_opcode, null, "N");
    }

    @Test
    void CCF_does_not_change_SF_ZF_PF() {
        AssertDoesNotChangeFlags(CCF_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    void CCF_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(CCF_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    void CCF_returns_proper_T_states() {
        var states = Execute(CCF_opcode, null);
        assertEquals(4, states);
    }
}
