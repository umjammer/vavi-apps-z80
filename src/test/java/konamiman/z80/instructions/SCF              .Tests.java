package konamiman.z80.instructions;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SCF_tests extends InstructionsExecutionTestsBase {

    private static final byte SCF_opcode = 0x37;

    @Test
    public void SCF_sets_CF_correctly() {
        registers.setCF(Bit.OFF);

        execute(SCF_opcode, null);

        assertEquals(1, registers.getCF().intValue());
    }

    @Test
    public void SCF_resets_H_and_N() {
        assertResetsFlags(SCF_opcode, null, "H", "N");
    }

    @Test
    public void SCF_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(SCF_opcode, null, "S", "Z", "P");
    }

    @Test
    public void SCF_sets_bits_3_and_5_from_A() {
        registers.setA(withBit(registers.getA(), 3, 1));
        registers.setA(withBit(registers.getA(), 5, 0));
        execute(SCF_opcode, null);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        registers.setA(withBit(registers.getA(), 3, 0));
        registers.setA(withBit(registers.getA(), 5, 1));
        execute(SCF_opcode, null);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0x0F, 0xF0, 0xFF})
    public void SCF_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(SCF_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void SCF_returns_proper_T_states() {
        var states = execute(SCF_opcode, null);
        assertEquals(4, states);
    }
}
