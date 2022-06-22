package konamiman.InstructionsExecution;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SCF_tests extends InstructionsExecutionTestsBase {

    private static final byte SCF_opcode = 0x37;

    @Test
    public void SCF_sets_CF_correctly() {
        Registers.setCF(Bit.OFF);

        Execute(SCF_opcode, null);

        assertEquals(1, Registers.getCF().intValue());
    }

    @Test
    public void SCF_resets_H_and_N() {
        AssertResetsFlags(SCF_opcode, null, "H", "N");
    }

    @Test
    public void SCF_does_not_change_SF_ZF_PF() {
        AssertDoesNotChangeFlags(SCF_opcode, null, "S", "Z", "P");
    }

    @Test
    public void SCF_sets_bits_3_and_5_from_A() {
        Registers.setA(WithBit(Registers.getA(), 3, 1));
        Registers.setA(WithBit(Registers.getA(), 5, 0));
        Execute(SCF_opcode, null);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Registers.setA(WithBit(Registers.getA(), 3, 0));
        Registers.setA(WithBit(Registers.getA(), 5, 1));
        Execute(SCF_opcode, null);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void SCF_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(SCF_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    public void SCF_returns_proper_T_states() {
        var states = Execute(SCF_opcode, null);
        assertEquals(4, states);
    }
}
