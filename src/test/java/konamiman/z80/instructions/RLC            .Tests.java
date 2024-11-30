package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class RLC_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RLC_Source() {
        return getBitInstructionsSource((byte) 0x00, true, false).stream();
    }

    private byte offset;

    @BeforeEach
    @Override
    protected void setup() {
        super.setup();
        offset = fixture.create(Byte.TYPE);
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_rotates_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {0xA, 0x14, 0x28, 0x50, (byte) 0xA0, 0x41, (byte) 0x82, 0x05};
        setupRegOrMem(reg, (byte) 0x05, offset);

        for (byte value : values) {
            executeBit(opcode, prefix, offset);
            assertEquals(value, valueOfRegOrMem(reg, offset));
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, valueOfRegOrMem(destReg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_sets_CF_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
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
    @MethodSource("RLC_Source")
    public void RLC_resets_H_and_N(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_sets_SF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) 0x20, offset);

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getSF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getSF().intValue());

        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff], registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_sets_bits_3_and_5_from_A(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            var value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), registers.getFlag3());
            assertEquals(getBit(value, 5), registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("RLC_Source")
    public void RLC_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}
