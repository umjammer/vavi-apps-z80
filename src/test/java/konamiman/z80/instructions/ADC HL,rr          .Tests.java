package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.createShort;
import static konamiman.z80.utils.NumberUtils.withBit;
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
            var value1 = fixture.create(Short.TYPE);
            var value2 = (src.equals("HL")) ? value1 : fixture.create(Short.TYPE);

            registers.setHL(value1);
            registers.setCF(Bit.of(cf));
            if (!src.equals("HL"))
                setReg(src, value2);

            execute(opcode, prefix);

            assertEquals(NumberUtils.add(NumberUtils.add(value1, value2), cf), registers.getHL());
            if (!src.equals("HL"))
                assertEquals(value2, this.<Short>getReg(src));
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_SF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFD, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (short) 0x7FFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (short) 0x7FFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());
    }

    private void setup(String src, short oldValue, short valueToSubtract) {
        registers.setHL(oldValue);
        registers.setCF(Bit.OFF);

        if (!src.equals("HL")) {
            setReg(src, valueToSubtract);
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_ZF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFD, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getZF().intValue());

        setup(src, (short) 0x00, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_HF_appropriately(String src, byte opcode) {
        for (int i : new int[] {0x0FFE, 0x7FFE, 0xEFFE}) {
            short s = (short) i;

            setup(src, s, (short) 1);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            setup(src, (short) (s + 1), (short) 1);
            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            setup(src, (short) (s + 2), (short) 1);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_CF_appropriately(String src, byte opcode) {
        setup(src, (short) 0xFFFE, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());

        setup(src, (short) 0xFFFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getCF().intValue());

        setup(src, (short) 0, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_PF_appropriately(String src, byte opcode) {
        // http://stackoverflow.com/a/8037485/4574

        testPF(src, opcode, 127, 0, 0);
        testPF(src, opcode, 127, 1, 1);
        testPF(src, opcode, 127, 127, 1);
        testPF(src, opcode, 127, 128, 0);
        testPF(src, opcode, 127, 129, 0);
        testPF(src, opcode, 127, 255, 0);
        testPF(src, opcode, 128, 0, 0);
        testPF(src, opcode, 128, 1, 0);
        testPF(src, opcode, 128, 127, 0);
        testPF(src, opcode, 128, 128, 1);
        testPF(src, opcode, 128, 129, 1);
        testPF(src, opcode, 128, 255, 1);
        testPF(src, opcode, 129, 0, 0);
        testPF(src, opcode, 129, 1, 0);
        testPF(src, opcode, 129, 127, 0);
        testPF(src, opcode, 129, 128, 1);
        testPF(src, opcode, 129, 129, 1);
        testPF(src, opcode, 129, 255, 0);
    }

    void testPF(String src, byte opcode, int oldValue, int subtractedValue, int expectedPF) {
        setup(src, createShort((byte) 0, (byte) oldValue), createShort((byte) 0, (byte) subtractedValue));

        execute(opcode, prefix);
        assertEquals(expectedPF, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_resets_NF(String src, byte opcode) {
        assertResetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_sets_bits_3_and_5_from_high_byte_of_result(String src, byte opcode) {
        registers.setHL(createShort((byte) 0, withBit(withBit(((byte) 0), 3, 1), 5, 0)));
        setReg(src, (short) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        registers.setHL(createShort((byte) 0, withBit(withBit(((byte) 0), 3, 0), 5, 1)));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADC_HL_rr_Source")
    public void ADC_HL_rr_returns_proper_T_states(String src, byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(15, states);
    }
}
