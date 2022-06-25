package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DEC_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> DEC_r_Source() {
        return Stream.of(
                arguments("A", (byte) 0x3D, null),
                arguments("B", (byte) 0x05, null),
                arguments("C", (byte) 0x0D, null),
                arguments("D", (byte) 0x15, null),
                arguments("E", (byte) 0x1D, null),
                arguments("H", (byte) 0x25, null),
                arguments("L", (byte) 0x2D, null),
                arguments("IXH", (byte) 0x25, (byte) 0xDD),
                arguments("IXL", (byte) 0x2D, (byte) 0xDD),
                arguments("IYH", (byte) 0x25, (byte) 0xFD),
                arguments("IYL", (byte) 0x2D, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_decreases_value_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x01);
        execute(opcode, prefix);
        assertEquals((byte) 0x00, this.<Byte>getReg(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.<Byte>getReg(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0xFE, this.<Byte>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x02);

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x03);

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getZF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            setReg(reg, b);

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x81);

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_NF(String reg, byte opcode, Byte prefix) {
        var randomValues = fixture.create(byte[].class);

        for (var value : randomValues) {
            setReg(reg, value);
            registers.setNF(Bit.OFF);

            execute(opcode, prefix);
            assertEquals(1, registers.getNF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_does_not_chance_CF(String reg, byte opcode, Byte prefix) {
        var randomValues = fixture.create(byte[].class);

        for (var value : randomValues) {
            setReg(reg, value);

            registers.setCF(Bit.OFF);
            execute(opcode, prefix);
            assertEquals(0, registers.getCF().intValue());

            registers.setCF(Bit.ON);
            execute(opcode, prefix);
            assertEquals(1, registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setReg(reg, withBit(withBit(((byte) 1), 3, 1), 5, 0));
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setReg(reg, withBit(withBit(((byte) 1), 3, 0), 5, 1));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 8, 4), states);
    }
}

