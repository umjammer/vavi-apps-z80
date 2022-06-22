package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RL_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0x10, /*includeLoadReg:*/ true, /*loopSevenBits:*/ false)));
    }

    private byte offset;

    @BeforeEach
    public void Setup() {
        offset = Fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_rotates_byte_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        SetupRegOrMem(reg, (byte) 0x03, offset);

        for (byte value : values) {
            ExecuteBit(opcode, prefix, offset);
            assertEquals(value, ValueOfRegOrMem(reg, offset) & 0xFE);
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bit_0_from_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) | 1), offset);
        Registers.setCF(Bit.OFF);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, GetBit(ValueOfRegOrMem(reg, offset), 0).intValue());

        SetupRegOrMem(reg, (byte) (Fixture.create(Byte.TYPE) & 0xFE), offset);
        Registers.setCF(Bit.ON);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, GetBit(ValueOfRegOrMem(reg, offset), 0).intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) 0x60, offset);

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getCF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getCF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getCF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertResetsFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) 0x20, offset);

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getSF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getSF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(ValueOfRegOrMem(reg, offset) == 0, Registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(Parity[ValueOfRegOrMem(reg, offset)], Registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            SetupRegOrMem(reg, b, offset);
            ExecuteBit(opcode, prefix, offset);
            var value = ValueOfRegOrMem(reg, offset);
            assertEquals(GetBit(value, 3), Registers.getFlag3());
            assertEquals(GetBit(value, 5), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = ExecuteBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
