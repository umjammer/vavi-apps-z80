package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class BIT_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> BIT_Source() {
        return getBitInstructionsSource((byte) 0x40, false, true).stream();
    }

    private byte offset;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_gets_bit_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var value = withBit(((byte) 0), bit, 1);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getZF().intValue());

        value = withBit(((byte) 0xFF), bit, 0);
        setupRegOrMem(reg, value, offset);
        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_PF_as_ZF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(registers.getZF(), registers.getPF());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_SF_if_bit_is_7_and_is_set(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            var b = (byte) i;
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            var expected = (bit == 7 && getBit(b, 7).intValue() == 1);
            assertEquals(expected, registers.getSF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_resets_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_sets_H(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertSetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_does_not_modify_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertDoesNotChangeFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("BIT_Source")
    public void BIT_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 12 : reg.startsWith("(") ? 20 : 8, states);
    }
}
