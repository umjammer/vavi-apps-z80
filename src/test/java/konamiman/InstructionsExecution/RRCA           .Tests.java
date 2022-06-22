package konamiman.InstructionsExecution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RRCA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRCA_opcode = 0x0F;

    @Test
    public void RRCA_rotates_byte_correctly() {
        var values = new byte[] {(byte) 0x82, 0x41, (byte) 0xA0, 0x50, 0x28, 0x14, 0x0A, 0x05};
        Registers.setA((byte) 0x05);

        for (byte value : values) {
            Execute(RRCA_opcode, null);
            assertEquals(value, Registers.getA());
        }
    }

    @Test
    public void RRCA_sets_CF_correctly() {
        Registers.setA((byte) 0x06);

        Execute(RRCA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());

        Execute(RRCA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RRCA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RRCA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());
    }

    @Test
    public void RRCA_resets_H_and_N() {
        AssertResetsFlags(RRCA_opcode, null, "H", "N");
    }

    @Test
    public void RRCA_does_not_change_SF_ZF_PF() {
        AssertDoesNotChangeFlags(RRCA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRCA_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(RRCA_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    public void RRCA_returns_proper_T_states() {
        var states = Execute(RRCA_opcode, null);
        assertEquals(4, states);
    }
}
