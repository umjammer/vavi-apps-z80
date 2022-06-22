package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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
        SetReg(reg, (byte) 0x01);
        Execute(opcode, prefix);
        assertEquals((byte) 0x00, this.<Byte>GetReg(reg));

        Execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.<Byte>GetReg(reg));

        Execute(opcode, prefix);
        assertEquals((byte) 0xFE, this.<Byte>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0x02);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0x03);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getZF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            SetReg(reg, b);

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0x81);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_NF(String reg, byte opcode, Byte prefix) {
        var randomValues = Fixture.create(byte[].class);

        for (var value : randomValues) {
            SetReg(reg, value);
            Registers.setNF(Bit.OFF);

            Execute(opcode, prefix);
            assertEquals(1, Registers.getNF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_does_not_chance_CF(String reg, byte opcode, Byte prefix) {
        var randomValues = Fixture.create(byte[].class);

        for (var value : randomValues) {
            SetReg(reg, value);

            Registers.setCF(Bit.OFF);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getCF().intValue());

            Registers.setCF(Bit.ON);
            Execute(opcode, prefix);
            assertEquals(1, Registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        SetReg(reg, WithBit(WithBit(((byte) 1), 3, 1), 5, 0));
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        SetReg(reg, WithBit(WithBit(((byte) 1), 3, 0), 5, 1));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("DEC_r_Source")
    public void DEC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(IfIndexRegister(reg, 8, /*@else:*/ 4), states);
    }
}

