package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static konamiman.DataTypesAndUtils.NumberUtils.inc7Bits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_A_I_R_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> LD_A_R_I_Source() {
        return Stream.of(
                arguments("I", (byte) 0x57),
                arguments("R", (byte) 0x5F)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_loads_value_correctly(String reg, byte opcode) {
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);
        Registers.setA(oldValue);
        SetReg(reg, newValue);

        Execute(opcode, prefix);

        //Account for R being increased on instruction execution
        if (reg.equals("R"))
            newValue = inc7Bits(inc7Bits(newValue));

        assertEquals(newValue, Registers.getA());
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_returns_proper_T_states(String reg, byte opcode) {
        var states = Execute(opcode, prefix);
        assertEquals(9, states);
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_SF_properly(String reg, byte opcode) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            SetReg(reg, b);
            Execute(opcode, prefix);
            assertEquals((b & 0xff) >= 128, Registers.getSF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_ZF_properly(String reg, byte opcode) {
        for (int i = 0; i <= 255; i++) {
            var b = (byte) i;
            SetReg(reg, b);
            Execute(opcode, prefix);

            //Account for R being increased on instruction execution
            if (reg.equals("R"))
                b = inc7Bits(inc7Bits(b));

            assertEquals(b == 0, Registers.getZF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_PF_from_IFF2(String reg, byte opcode) {
        SetReg(reg, Fixture.create(Byte.TYPE));

        Registers.setIFF2(Bit.OFF);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Registers.setIFF2(Bit.ON);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_resets_HF_and_NF_properly(String reg, byte opcode) {
        AssertResetsFlags(opcode, prefix, "H", "N");
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_does_not_change_CF(String reg, byte opcode) {
        AssertDoesNotChangeFlags(opcode, prefix, "C");
    }

    @ParameterizedTest
    @MethodSource("LD_A_R_I_Source")
    public void LD_A_I_R_sets_flags_3_5_from_I(String reg, byte opcode) {
        SetReg(reg, WithBit(WithBit(((byte) 1), 3, 1), 5, 0));
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        SetReg(reg, WithBit(WithBit(((byte) 1), 3, 0), 5, 1));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }
}
