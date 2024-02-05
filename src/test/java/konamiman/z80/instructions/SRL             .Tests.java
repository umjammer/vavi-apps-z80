package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;


class SRL_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> SRL_Source() {
        return getBitInstructionsSource((byte) 0x38, true, false).stream();
    }

    private byte offset;

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_shifts_byte_and_loads_register_correctly(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var values = new byte[] {0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01, 0};
        setupRegOrMem(reg, (byte) 0xFF, offset);

        for (byte value : values) {
            registers.setCF(Bit.ON);
            executeBit(opcode, prefix, offset);
            assertEquals(value, valueOfRegOrMem(reg, offset));
            if (destReg != null && !destReg.isEmpty())
                assertEquals(value, valueOfRegOrMem(destReg, offset));
        }
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_sets_CF_from_bit_0(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) | 0x01), offset);
        registers.setCF(Bit.OFF);
        executeBit(opcode, prefix, offset);
        assertEquals(1, registers.getCF().intValue());

        setupRegOrMem(reg, (byte) (fixture.create(Byte.TYPE) & 0xFE), offset);
        registers.setCF(Bit.ON);
        executeBit(opcode, prefix, offset);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_resets_H_N_and_S(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        assertResetsFlags(() -> executeBit(opcode, prefix, offset), opcode, prefix, "H", "N", "S");
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_sets_ZF_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(valueOfRegOrMem(reg, offset) == 0, registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_sets_PV_appropriately(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (int i = 0; i < 256; i++) {
            setupRegOrMem(reg, (byte) i, offset);
            executeBit(opcode, prefix, offset);
            assertEquals(parity[valueOfRegOrMem(reg, offset) & 0xff], registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_sets_bits_3_and_5_from_result(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        for (var b : new byte[] {0x00, (byte) 0xD7, 0x28, (byte) 0xFF}) {
            setupRegOrMem(reg, b, offset);
            executeBit(opcode, prefix, offset);
            var value = valueOfRegOrMem(reg, offset);
            assertEquals(getBit(value, 3), registers.getFlag3());
            assertEquals(getBit(value, 5), registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource("SRL_Source")
    public void SRL_returns_proper_T_states(String reg, String destReg, byte opcode, Byte prefix, int bit) {
        var states = executeBit(opcode, prefix, offset);
        assertEquals(reg.equals("(HL)") ? 15 : reg.startsWith("(I") ? 23 : 8, states);
    }
}