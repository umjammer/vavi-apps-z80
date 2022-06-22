package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class SRA_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> SRA_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0x28, /*includeLoadReg:*/ true, /*loopSevenBits:*/ false)));
    }

    private byte offset;

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_shifts_negative_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {(byte) 0xC0, (byte) 0xE0, (byte) 0xF0, (byte) 0xF8, (byte) 0xFC, (byte) 0xFE, (byte) 0xFF};
        SetupRegOrMem(reg, (byte) 0x80, offset);

        for (byte value : values) {
            Registers.setCF(Bit.ON);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(value, ValueOfRegOrMem(reg, offset));
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, ValueOfRegOrMem(destReg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_shifts_positive_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {0x20, 0x10, 0x08, 0x04, 0x02, 0x01, 0};
        SetupRegOrMem(reg, (byte) 0x40, offset);

        for (byte value : values) {
            Registers.setCF(Bit.ON);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(value, ValueOfRegOrMem(reg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_CF_from_bit_0(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) | 0x01), offset);
        Registers.setCF(Bit.OFF);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getCF().intValue());

        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) & 0xFE), offset);
        Registers.setCF(Bit.ON);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertResetsFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) | 0x80), offset);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getSF().intValue());

        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) & 0x7F), offset);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(ValueOfRegOrMem(reg, offset) == 0, Registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(Parity[ValueOfRegOrMem(reg, offset)], Registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            SetupRegOrMem(reg, b, offset);
            ExecuteBit(opcode, prefix, offset);
            var value = ValueOfRegOrMem(reg, offset);
            assertEquals(GetBit(value, 3), Registers.getFlag3());
            assertEquals(GetBit(value, 5), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("SRA_Source")
    public void SRA_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = ExecuteBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}