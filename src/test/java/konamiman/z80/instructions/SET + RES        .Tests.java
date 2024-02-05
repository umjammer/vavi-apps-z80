package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SET_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> SET_Source() {
        return getBitInstructionsSource((byte) 0xC0, true, true).stream();
    }

    static Stream<Arguments> RES_Source() {
        return getBitInstructionsSource((byte) 0x80, true, true).stream();
    }

    private byte offset;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("SET_Source")
    public void SET_sets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = withBit(fixture.create(Byte.TYPE), bit, 0);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        var expected = withBit(value, bit, 1);
        var actual = valueOfRegOrMem(reg, offset);
        assertEquals(expected, actual);
        if (destReg != null && !destReg.isEmpty())
            assertEquals(expected, valueOfRegOrMem(destReg, actual));
    }

    @ParameterizedTest
    @MethodSource("RES_Source")
    public void RES_resets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = withBit(fixture.create(Byte.TYPE), bit, 1);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        var expected = withBit(value, bit, 0);
        var actual = valueOfRegOrMem(reg, offset);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource({"SET_Source", "RES_Source"})
    public void SET_RES_do_not_change_flags(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertDoesNotChangeFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource({"SET_Source", "RES_Source"})
    public void SET_RES_return_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
