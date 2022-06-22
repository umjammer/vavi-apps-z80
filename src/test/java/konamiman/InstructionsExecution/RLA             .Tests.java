package konamiman.InstructionsExecution;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RLA_tests extends InstructionsExecutionTestsBase {

    private static final byte RLA_opcode = 0x17;

    @Test
    public void RLA_rotates_byte_correctly() {
        var values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        Registers.setA((byte) 0x03);

        for (byte value : values) {
            Execute(RLA_opcode, null);
            assertEquals(value, Registers.getA() & 0xFE);
        }
    }

    @Test
    public void RLA_sets_bit_0_from_CF() {
        Registers.setA((byte) (Fixture.create(Byte.TYPE) | 1));
        Registers.setCF(Bit.OFF);
        Execute(RLA_opcode, null);
        assertEquals(0, GetBit(Registers.getA(), 0).intValue());

        Registers.setA((byte) (Fixture.create(Byte.TYPE) & 0xFE));
        Registers.setCF(Bit.ON);
        Execute(RLA_opcode, null);
        assertEquals(1, GetBit(Registers.getA(), 0).intValue());
    }

    @Test
    public void RLA_sets_CF_correctly() {
        Registers.setA((byte) 0x60);

        Execute(RLA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());

        Execute(RLA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RLA_opcode, null);
        assertEquals(1, Registers.getCF().intValue());

        Execute(RLA_opcode, null);
        assertEquals(0, Registers.getCF().intValue());
    }

    @Test
    public void RLA_resets_H_and_N() {
        AssertResetsFlags(RLA_opcode, null, "H", "N");
    }

    @Test
    public void RLA_does_not_change_SF_ZF_PF() {
        AssertDoesNotChangeFlags(RLA_opcode, null, "S", "Z", "P");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x00, 0xD7, 0x28, 0xFF})
    public void RLA_sets_bits_3_and_5_from_A(int value) {
        Registers.setA((byte) value);
        Execute(RLA_opcode, null);
        assertEquals(GetBit(Registers.getA(), 3), Registers.getFlag3());
        assertEquals(GetBit(Registers.getA(), 5), Registers.getFlag5());
    }

    @Test
    public void RLA_returns_proper_T_states() {
        var states = Execute(RLA_opcode, null);
        assertEquals(4, states);
    }
}
