package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RRC_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RRC_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0x08, /*includeLoadReg:*/ true, /*loopSevenBits:*/ false)));
    }

    private byte offset;

    @BeforeEach
    public void Setup() {
        offset = Fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_rotates_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {(byte) 0x82, 0x41, (byte) 0xA0, 0x50, 0x28, 0x14, 0x0A, 0x05};
        SetupRegOrMem(reg, (byte) 0x05, offset);

        for (byte value : values) {
            ExecuteBit(opcode, prefix, offset);
            assertEquals(value, ValueOfRegOrMem(reg, offset));
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, ValueOfRegOrMem(destReg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) 0x06, offset);

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
    @MethodSource("RRC_Source")
    public void RRC_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertResetsFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        SetupRegOrMem(reg, (byte) 0x02, offset);

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getSF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getSF().intValue());

        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(ValueOfRegOrMem(reg, offset) == 0, Registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(Parity[ValueOfRegOrMem(reg, offset)], Registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_sets_bits_3_and_5_from_A(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            SetupRegOrMem(reg, b, offset);
            ExecuteBit(opcode, prefix, offset);
            var value = ValueOfRegOrMem(reg, offset);
            assertEquals(GetBit(value, 3), Registers.getFlag3());
            assertEquals(GetBit(value, 5), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("RRC_Source")
    public void RRC_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = ExecuteBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
