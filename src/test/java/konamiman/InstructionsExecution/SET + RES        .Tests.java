package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class SET_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> SET_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0xC0, /*includeLoadReg:*/ true, /*loopSevenBits:*/ true)));
    }

    static Stream<Arguments> RES_Source() {
        return Stream.of(arguments(GetBitInstructionsSource((byte) 0x80, /*includeLoadReg:*/ true, /*loopSevenBits:*/ true)));
    }

    private byte offset;

    @BeforeEach
    public void Setup() {
        offset = Fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("SET_Source")
    public void SET_sets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = WithBit(Fixture.create(Byte.TYPE), bit, 0);
        SetupRegOrMem(reg, value, offset);
        ExecuteBit(opcode, prefix, offset);
        var expected = WithBit(value, bit, 1);
        var actual = ValueOfRegOrMem(reg, offset);
        assertEquals(expected, actual);
        if (destReg != null && !destReg.isEmpty())
            assertEquals(expected, ValueOfRegOrMem(destReg, actual));
    }

    @ParameterizedTest
    @MethodSource("RES_Source")
    public void RES_resets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = WithBit(Fixture.create(Byte.TYPE), bit, 1);
        SetupRegOrMem(reg, value, offset);
        ExecuteBit(opcode, prefix, offset);
        var expected = WithBit(value, bit, 0);
        var actual = ValueOfRegOrMem(reg, offset);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource({"SET_Source", "RES_Source"})
    public void SET_RES_do_not_change_flags(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        AssertDoesNotChangeFlags(() -> ExecuteBit(opcode, prefix, offset), opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource({"SET_Source", "RES_Source"})
    public void SET_RES_return_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = ExecuteBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
