package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class BIT_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> BIT_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0x40, /*includeLoadReg:*/ false, /*loopSevenBits:*/ true)));
    }

    private byte offset;

    @BeforeEach
    public void Setup() {
        offset = Fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_gets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = WithBit(((byte) 0), bit, 1);
        SetupRegOrMem(reg, value, offset);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(0, Registers.getZF().intValue());

        value = WithBit(((byte) 0xFF), bit, 0);
        SetupRegOrMem(reg, value, offset);
        ExecuteBit(opcode, prefix, offset);
        assertEquals(1, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_PF_as_ZF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            SetupRegOrMem(reg, (byte) i, offset);
            ExecuteBit(opcode, prefix, offset);
            assertEquals(Registers.getZF(), Registers.getPF());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_SF_if_bit_is_7_and_is_set(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            var b = (byte) i;
            SetupRegOrMem(reg, b, offset);
            ExecuteBit(opcode, prefix, offset);
            var expected = (bit == 7 && GetBit(b, 7).intValue() == 1);
            assertEquals(expected, Registers.getSF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_resets_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertResetsFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_H(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertSetsFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "H");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_does_not_modify_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertDoesNotChangeFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = ExecuteBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 12 : reg.startsWith("(") ? 20 : 8, states);
    }
}
