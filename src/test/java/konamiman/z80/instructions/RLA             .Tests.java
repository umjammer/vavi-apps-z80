package konamiman.z80.instructions;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RLA_tests extends InstructionsExecutionTestsBase {

    private static final byte RLA_opcode = 0x17;

    @Test
    public void RLA_rotates_byte_correctly() {
        var values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        registers.setA((byte) 0x03);

        for (byte value : values) {
            execute(RLA_opcode, null);
            assertEquals(value, (byte) (registers.getA() & 0xFE));
        }
    }

    @Test
    public void RLA_sets_bit_0_from_CF() {
        registers.setA((byte) (fixture.create(Byte.TYPE) | 1));
        registers.setCF(Bit.OFF);
        execute(RLA_opcode, null);
        assertEquals(0, getBit(registers.getA(), 0).intValue());

        registers.setA((byte) (fixture.create(Byte.TYPE) & 0xFE));
        registers.setCF(Bit.ON);
        execute(RLA_opcode, null);
        assertEquals(1, getBit(registers.getA(), 0).intValue());
    }

    @Test
    public void RLA_sets_CF_correctly() {
        registers.setA((byte) 0x60);

        execute(RLA_opcode, null);
        assertEquals(0, registers.getCF().intValue());

        execute(RLA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RLA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RLA_opcode, null);
        assertEquals(0, registers.getCF().intValue());
    }

    @Test
    public void RLA_resets_H_and_N() {
        assertResetsFlags(RLA_opcode, null, "H", "N");
    }

    @Test
    public void RLA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RLA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RLA_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(RLA_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void RLA_returns_proper_T_states() {
        var states = execute(RLA_opcode, null);
        assertEquals(4, states);
    }
}
