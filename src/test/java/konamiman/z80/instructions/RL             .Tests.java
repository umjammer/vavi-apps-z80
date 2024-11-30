package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RL_Source() {
        return getBitInstructionsSource((byte) 0x10, true, false).stream();
    }

    private byte offset;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_rotates_byte_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {0x6, 0xC, 0x18, 0x30, 0x60, (byte) 0xC0, (byte) 0x80, 0};
        setupRegOrMem(reg, (byte) 0x03, offset);

        for (byte value : values) {
            executeBit(opcode, prefix, offset);
            assertEquals(value & 0xff, valueOfRegOrMem(reg, offset) & 0xFE);
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bit_0_from_CF(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 1), offset);
        registers.setCF(Bit.OFF);
        executeBit(opcode, prefix, offset);
        assertEquals(0, getBit(valueOfRegOrMem(reg, offset), 0).intValue());

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0xFE), offset);
        registers.setCF(Bit.ON);
        executeBit(opcode, prefix, offset);
        assertEquals(1, getBit(valueOfRegOrMem(reg, offset), 0).intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x60, offset);

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getCF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getCF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getCF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x20, offset);

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getSF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getSF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff], registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            var value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), registers.getFlag3());
            assertEquals(getBit(value, 5), registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("RL_Source")
    public void RL_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
