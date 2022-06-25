package konamiman.z80.instructions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RRCA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRCA_opcode = 0x0F;

    @Test
    public void RRCA_rotates_byte_correctly() {
        final var values = new byte[] {(byte) 0x82, 0x41, (byte) 0xA0, 0x50, 0x28, 0x14, 0x0A, 0x05};
        registers.setA((byte) 0x05);

        for (byte value : values) {
            execute(RRCA_opcode, null);
            assertEquals(value, registers.getA());
        }
    }

    @Test
    public void RRCA_sets_CF_correctly() {
        registers.setA((byte) 0x06);

        execute(RRCA_opcode, null);
        assertEquals(0, registers.getCF().intValue());

        execute(RRCA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RRCA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RRCA_opcode, null);
        assertEquals(0, registers.getCF().intValue());
    }

    @Test
    public void RRCA_resets_H_and_N() {
        assertResetsFlags(RRCA_opcode, null, "H", "N");
    }

    @Test
    public void RRCA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RRCA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRCA_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(RRCA_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void RRCA_returns_proper_T_states() {
        var states = execute(RRCA_opcode, null);
        assertEquals(4, states);
    }
}
