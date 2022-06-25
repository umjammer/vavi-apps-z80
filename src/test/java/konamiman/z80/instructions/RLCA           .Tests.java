package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RLCA_tests extends InstructionsExecutionTestsBase {

    private static final byte RLCA_opcode = 0x07;

    @Test
    public void RLCA_rotates_byte_correctly() {
        final var values = new byte[] {0xA, 0x14, 0x28, 0x50, (byte) 0xA0, 0x41, (byte) 0x82, 0x05};
        registers.setA((byte) 0x05);

        for (byte value : values) {
            execute(RLCA_opcode, null);
            assertEquals(value, registers.getA());
        }
    }

    @Test
    public void RLCA_sets_CF_correctly() {
        registers.setA((byte) 0x60);

        execute(RLCA_opcode, null);
        assertEquals(0, registers.getCF().intValue());

        execute(RLCA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RLCA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RLCA_opcode, null);
        assertEquals(0, registers.getCF().intValue());
    }

    @Test
    public void RLCA_resets_H_and_N() {
        assertResetsFlags(RLCA_opcode, null, "H", "N");
    }

    @Test
    public void RLCA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RLCA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RLCA_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(RLCA_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void RLCA_returns_proper_T_states() {
        var states = execute(RLCA_opcode, null);
        assertEquals(4, states);
    }
}
