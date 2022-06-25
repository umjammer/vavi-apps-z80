package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class INC_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> INC_r_Source() {
        return Stream.of(
                arguments("A", (byte) 0x3C, null),
                arguments("B", (byte) 0x04, null),
                arguments("C", (byte) 0x0C, null),
                arguments("D", (byte) 0x14, null),
                arguments("E", (byte) 0x1C, null),
                arguments("H", (byte) 0x24, null),
                arguments("L", (byte) 0x2C, null),
                arguments("IXH", (byte) 0x24, (byte) 0xDD),
                arguments("IXL", (byte) 0x2C, (byte) 0xDD),
                arguments("IYH", (byte) 0x24, (byte) 0xFD),
                arguments("IYL", (byte) 0x2C, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_increases_value_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFE);
        execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.<Byte>getReg(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0x00, this.<Byte>getReg(reg));

        execute(opcode, prefix);
        assertEquals((byte) 0x01, this.<Byte>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFD);

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0xFD);

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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        setReg(reg, (byte) 0x7E);

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_resets_NF(String reg, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        setReg(reg, withBit(withBit(((byte) 0), 3, 1), 5, 0));
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setReg(reg, withBit(withBit(((byte) 0), 3, 0), 5, 1));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 8, 4), states);
    }
}
