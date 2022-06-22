package konamiman.InstructionsExecution;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RRA_tests extends InstructionsExecutionTestsBase {

    private static final byte RRA_opcode = 0x1F;

    @Test
    public void RRA_rotates_byte_correctly() {
        var values = new byte[] {0x60, 0x30, 0x18, 0xC, 0x6, 0x3, 0x1, 0x0};
        Registers.setA((byte) 0xC0);

        for (byte value : values) {
            Execute(RRA_opcode, null);
            assertEquals(value, Registers.getA() & 0x7F);
        }
    }

    @Test
    public void RLA_sets_bit_7_from_CF() {
        Registers.setA((byte) (Fixture.create(Byte.TYPE) | 0x80));
        Registers.setCF(Bit.OFF);
        Execute(RRA_opcode, null);
        assertEquals(0, GetBit(Registers.getA(), 7).intValue());

        Registers.setA((byte) (Fixture.create(Byte.TYPE) & 0x7F));
        Registers.setCF(Bit.ON);
        Execute(RRA_opcode, null);
        assertEquals(1, GetBit(Registers.getA(), 7).intValue());
    }

    @Test
    public void RRA_sets_CF_correctly() {
        Registers.setA((byte) 0x06);

        Execute(RRA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());

        Execute(RRA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RRA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RRA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());
    }

    @Test
    public void RRA_resets_H_and_N() {
        AssertResetsFlags(RRA_opcode, null, "H", "N");
    }

    @Test
    public void RRA_does_not_change_SF_ZF_PF() {
        AssertDoesNotChangeFlags(RRA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RRA_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(RRA_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    public void RRA_returns_proper_T_states() {
        var states = Execute(RRA_opcode, null);
        assertEquals(4, states);
    }
}
