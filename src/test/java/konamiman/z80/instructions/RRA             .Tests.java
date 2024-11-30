package konamiman.z80.instructions;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RRA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRA_opcode = 0x1F;

    @Test
    public void RRA_rotates_byte_correctly() {
        var values = new byte[] {0x60, 0x30, 0x18, 0xC, 0x6, 0x3, 0x1, 0x0};
        registers.setA((byte) 0xC0);

        for (byte value : values) {
            execute(RRA_opcode, null);
            assertEquals(value, registers.getA() & 0x7F);
        }
    }

    @Test
    public void RLA_sets_bit_7_from_CF() {
        registers.setA((byte) (fixture.create(Byte.TYPE) | 0x80));
        registers.setCF(Bit.OFF);
        execute(RRA_opcode, null);
        assertEquals(0, getBit(registers.getA(), 7).intValue());

        registers.setA((byte) (fixture.create(Byte.TYPE) & 0x7F));
        registers.setCF(Bit.ON);
        execute(RRA_opcode, null);
        assertEquals(1, getBit(registers.getA(), 7).intValue());
    }

    @Test
    public void RRA_sets_CF_correctly() {
        registers.setA((byte) 0x06);

        execute(RRA_opcode, null);
        assertEquals(0, registers.getCF().intValue());

        execute(RRA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RRA_opcode, null);
        assertEquals(1, registers.getCF().intValue());

        execute(RRA_opcode, null);
        assertEquals(0, registers.getCF().intValue());
    }

    @Test
    public void RRA_resets_H_and_N() {
        assertResetsFlags(RRA_opcode, null, "H", "N");
    }

    @Test
    public void RRA_does_not_change_SF_ZF_PF() {
        assertDoesNotChangeFlags(RRA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRA_sets_bits_3_and_5_from_A(int value) {
        registers.setA((byte) value);
        execute(RRA_opcode, null);
        assertEquals(getBit(registers.getA(), 3), registers.getFlag3());
        assertEquals(getBit(registers.getA(), 5), registers.getFlag5());
    }

    @Test
    public void RRA_returns_proper_T_states() {
        var states = execute(RRA_opcode, null);
        assertEquals(4, states);
    }
}
