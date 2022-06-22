package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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
        SetReg(reg, (byte) 0xFE);
        Execute(opcode, prefix);
        assertEquals((byte) 0xFF, this.<Byte>GetReg(reg));

        Execute(opcode, prefix);
        assertEquals((byte) 0x00, this.<Byte>GetReg(reg));

        Execute(opcode, prefix);
        assertEquals((byte) 0x01, this.<Byte>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_SF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0xFD);

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_sets_ZF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0xFD);

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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_HF_appropriately(String reg, byte opcode, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_PF_appropriately(String reg, byte opcode, Byte prefix) {
        SetReg(reg, (byte) 0x7E);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_resets_NF(String reg, byte opcode, Byte prefix) {
        AssertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_does_not_change_CF(String reg, byte opcode, Byte prefix) {
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
    @MethodSource("INC_r_Source")
    public void INC_r_sets_bits_3_and_5_from_result(String reg, byte opcode, Byte prefix) {
        SetReg(reg, WithBit(WithBit(((byte) 0), 3, 1), 5, 0));
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        SetReg(reg, WithBit(WithBit(((byte) 0), 3, 0), 5, 1));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("INC_r_Source")
    public void INC_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(IfIndexRegister(reg, 8, /*@else:*/ 4), states);
    }
}
