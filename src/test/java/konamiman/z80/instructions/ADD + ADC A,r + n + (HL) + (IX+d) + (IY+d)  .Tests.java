package konamiman.z80.instructions;

import java.util.ArrayList;
import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ADDC_A_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> ADDC_A_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0x87, 0, null),
                arguments("A", (byte) 0x8F, 0, null),
                arguments("A", (byte) 0x8F, 1, null)
        );
    }

    static Stream<Arguments> ADDC_A_r_Source() {
        var combinations = new ArrayList<Arguments>();

        var registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n", "IXH", "IXL", "IYH", "IYL", "(IX+n)", "(IY+n)"};
        for (var src = 0; src < registers.length; src++) {
            var reg = registers[src];
            var i = new int[] {src};
            Byte[] prefix = new Byte[1];

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            var ADD_opcode = (byte) (i[0] == 7 ? 0xC6 : (i[0] | 0x80));
            var ADC_opcode = (byte) (i[0] == 7 ? 0xCE : (i[0] | 0x88));
            combinations.add(arguments(reg, ADD_opcode, 0, prefix[0]));
            combinations.add(arguments(reg, ADC_opcode, 0, prefix[0]));
            combinations.add(arguments(reg, ADC_opcode, 1, prefix[0]));
        }

        return combinations.stream();
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_adds_both_registers_with_or_without_carry(String src, byte opcode, int cf, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var valueToAdd = src.equals("A") ? oldValue : fixture.create(Byte.TYPE);

        Setup(src, oldValue, valueToAdd, cf);
        execute(opcode, prefix);

        assertEquals(add(oldValue, valueToAdd + cf), registers.getA());
    }

    private void Setup(String src, byte oldValue, byte valueToAdd, int cf/* = 0*/) {
        registers.setA(oldValue);
        registers.setCF(Bit.of(cf));

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToAdd);
        } else if (src.equals("(HL)")) {
            var address = fixture.create(Short.TYPE);
            processorAgent.writeToMemory(address, valueToAdd);
            registers.setHL(address);
        } else if (src.startsWith("(I")) {
            var address = fixture.create(Short.TYPE);
            var offset = fixture.create(Byte.TYPE);
            var realAddress = NumberUtils.add(address, offset);
            processorAgent.writeToMemory(realAddress, valueToAdd);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToAdd);
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_SF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFD, (byte) 1, 0);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_ZF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFD, (byte) 1, 0);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_HF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
            Setup(src, b, (byte) 1, 0);

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_PF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0x7E, (byte) 1, 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getPF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_resets_NF(String src, byte opcode, int cf, Byte prefix) {
        assertResetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_CF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFE, (byte) 1, 0);

        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());

        execute(opcode, prefix);
        assertEquals(1, registers.getCF().intValue());

        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_bits_3_and_5_from_result(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, withBit(withBit((byte) 0, 3, 1), 5, 0), (byte) 0, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        Setup(src, withBit(withBit((byte) 0, 3, 0), 5, 1), (byte) 0, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_returns_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }
}
