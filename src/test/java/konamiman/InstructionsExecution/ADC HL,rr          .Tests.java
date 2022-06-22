package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import konamiman.DataTypesAndUtils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ADC_HL_rr_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> ADC_HL_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x4A),
                arguments("DE", (byte) 0x5A),
                arguments("SP", (byte) 0x7A)
        );
    }

    static Stream<Arguments> ADC_HL_HL_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x6A)
        );
    }

    @ParameterizedTest
    @MethodSource({"ADC_HL_rr_Source", "ADC_HL_HL_Source"})
    public void ADC_HL_rr_adds_both_registers_with_and_without_carry(String src, byte opcode) {
        for (var cf = 0; cf <= 1; cf++) {
            var value1 = Fixture.create(Short.TYPE);
            var value2 = (src.equals("HL")) ? value1 : Fixture.create(Short.TYPE);

            Registers.setHL(value1);
            Registers.setCF(Bit.of(cf));
            if (!src.equals("HL"))
                SetReg(src, value2);

            Execute(opcode, prefix);

            assertEquals(Add(Add(value1, value2), (short) cf), Registers.getHL());
            if (!src.equals("HL"))
                assertEquals(value2, this.<Short>GetReg(src));
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_SF_appropriately(String src, byte opcode) {
        Setup(src, ToShort(0xFFFD), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Setup(src, ToShort(0xFFFE), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Setup(src, ToShort(0xFFFF), (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, ToShort(0x7FFE), (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, ToShort(0x7FFF), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());
    }

    private void Setup(String src, short oldValue, short valueToSubstract) {
        Registers.setHL(oldValue);
        Registers.setCF(Bit.OFF);

        if (!src.equals("HL")) {
            SetReg(src, valueToSubstract);
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_ZF_appropriately(String src, byte opcode) {
        Setup(src, ToShort(0xFFFD), (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, ToShort(0xFFFE), (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, ToShort(0xFFFF), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getZF().intValue());

        Setup(src, (short) 0x00, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_HF_appropriately(String src, byte opcode) {
        for (int i : new int[] {0x0FFE, 0x7FFE, 0xEFFE}) {
            short s = ToShort(i);

            Setup(src, s, (short) 1);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Setup(src, ToShort(s + 1), (short) 1);
            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Setup(src, ToShort(s + 2), (short) 1);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_CF_appropriately(String src, byte opcode) {
        Setup(src, ToShort(0xFFFE), (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());

        Setup(src, ToShort(0xFFFF), (short) 1);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getCF().intValue());

        Setup(src, (short) 0, (short) 1);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_PF_appropriately(String src, byte opcode) {
        // http://stackoverflow.com/a/8037485/4574

        TestPF(src, opcode, 127, 0, 0);
        TestPF(src, opcode, 127, 1, 1);
        TestPF(src, opcode, 127, 127, 1);
        TestPF(src, opcode, 127, 128, 0);
        TestPF(src, opcode, 127, 129, 0);
        TestPF(src, opcode, 127, 255, 0);
        TestPF(src, opcode, 128, 0, 0);
        TestPF(src, opcode, 128, 1, 0);
        TestPF(src, opcode, 128, 127, 0);
        TestPF(src, opcode, 128, 128, 1);
        TestPF(src, opcode, 128, 129, 1);
        TestPF(src, opcode, 128, 255, 1);
        TestPF(src, opcode, 129, 0, 0);
        TestPF(src, opcode, 129, 1, 0);
        TestPF(src, opcode, 129, 127, 0);
        TestPF(src, opcode, 129, 128, 1);
        TestPF(src, opcode, 129, 129, 1);
        TestPF(src, opcode, 129, 255, 0);
    }

    void TestPF(String src, byte opcode, int oldValue, int substractedValue, int expectedPF) {
        Setup(src, NumberUtils.createShort((byte) 0, (byte) oldValue), NumberUtils.createShort((byte) 0, (byte) substractedValue));

        Execute(opcode, prefix);
        assertEquals(expectedPF, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_resets_NF(String src, byte opcode) {
        AssertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_bits_3_and_5_from_high_byte_of_result(String src, byte opcode) {
        Registers.setHL(NumberUtils.createShort((byte) 0, WithBit(WithBit(((byte) 0), 3, 1), 5, 0)));
        SetReg(src, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Registers.setHL(NumberUtils.createShort((byte) 0, WithBit(WithBit(((byte) 0), 3, 0), 5, 1)));
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_returns_proper_T_states(String src, byte opcode) {
        var states = Execute(opcode, prefix);
        assertEquals(15, states);
    }
}
