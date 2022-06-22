package konamiman.InstructionsExecution;

import java.util.ArrayList;
import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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

            ModifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

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
        var oldValue = Fixture.create(Byte.TYPE);
        var valueToAdd = src.equals("A") ? oldValue : Fixture.create(Byte.TYPE);

        Setup(src, oldValue, valueToAdd, cf);
        Execute(opcode, prefix);

        assertEquals(Add(oldValue, valueToAdd + cf), Registers.getA());
    }

    private void Setup(String src, byte oldValue, byte valueToAdd, int cf/* = 0*/) {
        Registers.setA(oldValue);
        Registers.setCF(Bit.of(cf));

        if (src.equals("n")) {
            SetMemoryContentsAt((short) 1, valueToAdd);
        } else if (src.equals("(HL)")) {
            var address = Fixture.create(Short.TYPE);
            ProcessorAgent.getMemory()[address] = valueToAdd;
            Registers.setHL(ToShort(address));
        } else if (src.startsWith("(I")) {
            var address = Fixture.create(Short.TYPE);
            var offset = Fixture.create(Byte.TYPE);
            var realAddress = Add(address, ToSignedByte(offset));
            ProcessorAgent.getMemory()[realAddress] = valueToAdd;
            SetMemoryContentsAt((short) 2, offset);
            SetReg(src.substring(1, 1 + 2), ToShort(address));
        } else if (!src.equals("A")) {
            SetReg(src, valueToAdd);
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_SF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFD, (byte) 1, 0);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_ZF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFD, (byte) 1, 0);

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
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_HF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        for (byte b : new byte[] {0x0E, 0x7E, (byte) 0xFE}) {
            Setup(src, b, (byte) 1, 0);

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_PF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0x7E, (byte) 1, 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getPF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_resets_NF(String src, byte opcode, int cf, Byte prefix) {
        AssertResetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_CF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0xFE, (byte) 1, 0);

        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());

        Execute(opcode, prefix);
        assertEquals(1, Registers.getCF().intValue());

        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("ADDC_A_r_Source")
    public void ADDC_A_r_sets_bits_3_and_5_from_result(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, WithBit(WithBit((byte) 0, 3, 1), 5, 0), (byte) 0, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Setup(src, WithBit(WithBit((byte) 0, 3, 0), 5, 1), (byte) 0, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"ADDC_A_r_Source", "ADDC_A_A_Source"})
    public void ADDC_A_r_returns_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }
}
