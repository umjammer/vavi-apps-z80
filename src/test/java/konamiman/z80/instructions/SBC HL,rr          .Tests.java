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


class SBC_HL_rr_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> SBC_HL_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x42),
                arguments("DE", (byte) 0x52),
                arguments("SP", (byte) 0x72)
        );
    }

    static Stream<Arguments> SBC_HL_HL_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x62)
        );
    }

    @ParameterizedTest
    @MethodSource({"SBC_HL_rr_Source", "SBC_HL_HL_Source"})
    public void SBC_HL_rr_subtracts_both_registers_with_and_without_carry(String src, byte opcode) {
        for (var cf = 0; cf <= 1; cf++) {
            var value1 = fixture.create(Short.TYPE);
            var value2 = src.equals("HL") ? value1 : fixture.create(Short.TYPE);

            registers.setHL(value1);
            registers.setCF(Bit.of(cf));
            if (!src.equals("HL"))
                setReg(src, value2);

            execute(opcode, prefix);

            assertEquals(NumberUtils.sub(NumberUtils.sub(value1, value2 & 0xffff), cf), registers.getHL());
            if (!src.equals("HL"))
                assertEquals(value2, this.<Short>getReg(src));
        }
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_SF_appropriately(String src, byte opcode) {
        setup(src, (short) 0x02, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (short) 0x01, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (short) 0x00, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        setup(src, (short) 0xFFFF, (short) 1);
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
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_ZF_appropriately(String src, byte opcode) {
        setup(src, (short) 0x03, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (short) 0x02, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (short) 0x01, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getZF().intValue());

        setup(src, (short) 0x00, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_HF_appropriately(String src, byte opcode) {
        for (int i : new int[] {0x1001, 0x8001, 0xF001}) {
            short b = (short) i;

            setup(src, b, (short) 1);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            setup(src, (byte) (b - 1), (short) 1);
            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            setup(src, (byte) (b - 2), (short) 1);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SUB_HL_rr_sets_CF_appropriately(String src, byte opcode) {
        setup(src, (short) 0x01, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());

        setup(src, (short) 0x00, (short) 1);
        execute(opcode, prefix);
        assertEquals(1, registers.getCF().intValue());

        setup(src, (short) 0xFF, (short) 1);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_PF_appropriately(String src, byte opcode) {
        //http://stackoverflow.com/a/8037485/4574

        testPF(src, opcode, 127, 0, 0);
        testPF(src, opcode, 127, 1, 0);
        testPF(src, opcode, 127, 127, 0);
        testPF(src, opcode, 127, 128, 1);
        testPF(src, opcode, 127, 129, 1);
        testPF(src, opcode, 127, 255, 1);
        testPF(src, opcode, 128, 0, 0);
        testPF(src, opcode, 128, 1, 1);
        testPF(src, opcode, 128, 127, 1);
        testPF(src, opcode, 128, 128, 0);
        testPF(src, opcode, 128, 129, 0);
        testPF(src, opcode, 128, 255, 0);
        testPF(src, opcode, 129, 0, 0);
        testPF(src, opcode, 129, 1, 0);
        testPF(src, opcode, 129, 127, 1);
        testPF(src, opcode, 129, 128, 0);
        testPF(src, opcode, 129, 129, 0);
        testPF(src, opcode, 129, 255, 0);
    }

    void testPF(String src, byte opcode, int oldValue, int subtractedValue, int expectedPF) {
        setup(src, createShort((byte) 0, (byte) oldValue), createShort((byte) 0, (byte) subtractedValue));

        execute(opcode, prefix);
        assertEquals(expectedPF, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_NF(String src, byte opcode) {
        assertSetsFlags(opcode, prefix, "N");
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_sets_bits_3_and_5_from_high_byte_of_result(String src, byte opcode) {
        registers.setHL(createShort((byte) 0, withBit(withBit((byte) 0, 3, 1), 5, 0)));
        setReg(src, (short) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        registers.setHL(createShort((byte) 0, withBit(withBit((byte) 0, 3, 0), 5, 1)));
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource("SBC_HL_rr_Source")
    public void SBC_HL_rr_returns_proper_T_states(String src, byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(15, states);
    }
}

