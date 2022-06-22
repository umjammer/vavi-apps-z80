package konamiman.InstructionsExecution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Dec;
import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class SUB_SBC_CPI_CPD_CP_r_tests extends InstructionsExecutionTestsBase {

    private static final byte cpidrPrefix = (byte) 0xED;
    private final byte[] cpidrOpcodes = new byte[] {(byte) 0xA1, (byte) 0xA9, (byte) 0xB1, (byte) 0xFF};

    static Stream<Arguments> SUB_SBC_A_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0x97, 0, null),
                arguments("A", (byte) 0x9F, 0, null),
                arguments("A", (byte) 0x9F, 1, null)
        );
    }

    static Stream<Arguments> CP_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0xBF, 0, null)
        );
    }

    static Stream<Arguments> CPI_Source() {
        return Stream.of(
                arguments("CPI", (byte) 0xA1, 0, null)
        );
    }

    static Stream<Arguments> CPD_Source() {
        return Stream.of(
                arguments("CPD", (byte) 0xA9, 0, null)
        );
    }

    static Stream<Arguments> CPIR_Source() {
        return Stream.of(
                arguments("CPIR", (byte) 0xB1, 0, null)
        );
    }

    static Stream<Arguments> CPDR_Source() {
        return Stream.of(
                arguments("CPDR", (byte) 0xFF, 0, null) // can't use B9 because it's "CP C" without prefix
        );
    }

    public static List<Arguments> SUB_SBC_A_r;
    public static List<Arguments> CP_r;
    public static List<Arguments> CPID_R;

    static Stream<Arguments> SUB_SBC_A_r_Source() {
        return SUB_SBC_A_r.stream();
    }
    static Stream<Arguments> CP_r_Source() {
        return CP_r.stream();
    }
    static Stream<Arguments> CPID_R_Source() {
        return CPID_R.stream();
    }

    static {
        var combinations = new ArrayList<Arguments>();
        var CP_combinations = new ArrayList<Arguments>();

        var registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n", "IXH", "IXL", "IYH", "IYL", "(IX+n)", "(IY+n)"};
        for (var src = 0; src < registers.length; src++) {
            var reg = registers[src];
            var i = new int[] {src};
            Byte[] prefix = new Byte[1];

            ModifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            var SUB_opcode = (byte) (i[0] == 7 ? 0xD6 : (i[0] | 0x90));
            var SBC_opcode = (byte) (i[0] == 7 ? 0xDE : (i[0] | 0x98));
            var CP_opcode = (byte) (i[0] == 7 ? 0xFE : (i[0] | 0xB8));
            combinations.add(arguments(reg, SUB_opcode, 0, prefix[0]));
            combinations.add(arguments(reg, SBC_opcode, 0, prefix[0]));
            combinations.add(arguments(reg, SBC_opcode, 1, prefix[0]));
            CP_combinations.add(arguments(reg, CP_opcode, 0, prefix[0]));
        }

        SUB_SBC_A_r = combinations;
        CP_r = CP_combinations;

        CPID_R = Stream.of(CPI_Source(), CPD_Source(), CPIR_Source(), CPDR_Source()).flatMap(Function.identity()).collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source"})
    public void SUB_SBC_A_r_substracts_both_registers_with_or_without_carry(String src, byte opcode, int cf, Byte prefix) {
        var oldValue = Fixture.create(Byte.TYPE);
        var valueToSubstract = src.equals("A") ? oldValue : Fixture.create(Byte.TYPE);

        Setup(src, oldValue, valueToSubstract, cf);
        Execute(opcode, prefix);

        assertEquals(Sub(oldValue, valueToSubstract + cf), Registers.getA());
    }

    @ParameterizedTest
    @MethodSource({"CP_r_Source", "CP_A_Source", "CPID_R_Source"})
    public void CPs_do_not_change_A(String src, byte opcode, int cf, Byte prefix) {
        var oldValue = Fixture.create(Byte.TYPE);
        var argument = Fixture.create(Byte.TYPE);

        Setup(src, oldValue, argument, cf);
        Execute(opcode, prefix);

        assertEquals(oldValue, Registers.getA());
    }

    private void Setup(String src, byte oldValue, byte valueToSubstract, int cf/* = 0*/) {
        Registers.setA(oldValue);
        Registers.setCF(Bit.of(cf));

        if (src.equals("n")) {
            SetMemoryContentsAt((short) 1, valueToSubstract);
        } else if (src.equals("(HL)") || src.startsWith("CP")) {
            var address = Fixture.create(Short.TYPE);
            ProcessorAgent.getMemory()[address] = valueToSubstract;
            Registers.setHL(ToShort(address));
        } else if (src.startsWith("(I")) {
            var address = Fixture.create(Short.TYPE);
            var offset = Fixture.create(Byte.TYPE);
            var realAddress = Add(address, ToSignedByte(offset));
            ProcessorAgent.getMemory()[realAddress] = valueToSubstract;
            SetMemoryContentsAt((short) 2, offset);
            SetReg(src.substring(1, 1 + 2), ToShort(address));
        } else if (!src.equals("A")) {
            SetReg(src, valueToSubstract);
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_SF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0x02, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, (byte) 0x01, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getSF().intValue());

        Setup(src, (byte) 0x00, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());

        Setup(src, (byte) 0xFF, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_ZF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0x03, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, (byte) 0x02, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());

        Setup(src, (byte) 0x01, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getZF().intValue());

        Setup(src, (byte) 0x00, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_HF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            Setup(src, b, (byte) 1, 0);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());

            Setup(src, (byte) (b - 1), (byte) 1, 0);
            Execute(opcode, prefix);
            assertEquals(1, Registers.getHF().intValue());

            Setup(src, (byte) (b - 2), (byte) 1, 0);
            Execute(opcode, prefix);
            assertEquals(0, Registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_PF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        //http://stackoverflow.com/a/8037485/4574

        TestPF(src, opcode, 127, 0, 0, prefix);
        TestPF(src, opcode, 127, 1, 0, prefix);
        TestPF(src, opcode, 127, 127, 0, prefix);
        TestPF(src, opcode, 127, 128, 1, prefix);
        TestPF(src, opcode, 127, 129, 1, prefix);
        TestPF(src, opcode, 127, 255, 1, prefix);
        TestPF(src, opcode, 128, 0, 0, prefix);
        TestPF(src, opcode, 128, 1, 1, prefix);
        TestPF(src, opcode, 128, 127, 1, prefix);
        TestPF(src, opcode, 128, 128, 0, prefix);
        TestPF(src, opcode, 128, 129, 0, prefix);
        TestPF(src, opcode, 128, 255, 0, prefix);
        TestPF(src, opcode, 129, 0, 0, prefix);
        TestPF(src, opcode, 129, 1, 0, prefix);
        TestPF(src, opcode, 129, 127, 1, prefix);
        TestPF(src, opcode, 129, 128, 0, prefix);
        TestPF(src, opcode, 129, 129, 0, prefix);
        TestPF(src, opcode, 129, 255, 0, prefix);
    }

    void TestPF(String src, byte opcode, int oldValue, int substractedValue, int expectedPF, Byte prefix) {
        Setup(src, (byte) oldValue, (byte) substractedValue, 0);

        Execute(opcode, prefix);
        assertEquals(expectedPF, Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_sets_NF(String src, byte opcode, int cf, Byte prefix) {
        AssertSetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_CF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) 0x01, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());

        Setup(src, (byte) 0x00, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getCF().intValue());

        Setup(src, (byte) 0xFF, (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SUB_SBC_A_r_Source")
    public void SUB_SBC_r_sets_bits_3_and_5_from_result(String src, byte opcode, int cf, Byte prefix) {
        Setup(src, (byte) (WithBit(WithBit((byte) 0, 3, 1), 5, 0) + 1), (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        Setup(src, (byte) (WithBit(WithBit((byte) 0, 3, 0), 5, 1) + 1), (byte) 1, 0);
        Execute(opcode, prefix);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_returns_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPRD_do_not_change_C(String src, byte opcode, int cf, Byte prefix) {
        AssertDoesNotChangeFlags(opcode, cpidrPrefix, "C");
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_reset_PF_if_BC_reaches_zero(String src, byte opcode, int cf, Byte prefix) {
        Registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = Registers.getBC();
            Execute(opcode, prefix);
            assertEquals(Dec(oldBC), Registers.getBC());
            assertEquals(Registers.getBC() != 0, Registers.getPF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPIR_Source"})
    public void CPI_CPIR_increase_HL(String src, byte opcode, int cf, Byte prefix) {
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(address);

        Execute(opcode, prefix);

        assertEquals(Inc(address), Registers.getHL());
    }

    @ParameterizedTest
    @MethodSource({"CPD_Source", "CPDR_Source"})
    public void CPD_CPDR_decrease_HL(String src, byte opcode, int cf, Byte prefix) {
        var address = Fixture.create(Short.TYPE);

        Registers.setHL(address);

        Execute(opcode, prefix);

        assertEquals(Dec(address), Registers.getHL());
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_decrease_BC(String src, byte opcode, int cf, Byte prefix) {
        var count = Fixture.create(Short.TYPE);

        Registers.setBC(count);

        Execute(opcode, prefix);

        assertEquals(Dec(count), Registers.getBC());
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPD_Source"})
    public void CPI_CPD_return_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_set_Flag3_from_bit_3_of_A_minus_aHL_minus_HF_and_Flag5_from_bit_1(String src, byte opcode, int cf, Byte prefix) {
        var valueInMemory = Fixture.create(Byte.TYPE);
        var srcAddress = Fixture.create(Short.TYPE);

        for (int i = 0; i < 256; i++) {
            var valueOfA = (byte) i;
            Registers.setA(valueOfA);
            Registers.setHL(srcAddress);
            ProcessorAgent.getMemory()[srcAddress] = valueInMemory;

            Execute(opcode, prefix);

            var expected = GetLowByte(Sub(Sub(valueOfA, valueInMemory), (short) Registers.getHF().intValue()));
            assertEquals(GetBit(expected, 3), Registers.getFlag3());
            assertEquals(GetBit(expected, 1), Registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_Z_is_1_but_BC_is_not_0(String src, byte opcode, int cf, Byte prefix) {
        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var data = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = data;
        Registers.setA(data);
        Registers.setHL(ToShort(dataAddress));
        Registers.setBC(Fixture.create(Short.TYPE));

        ExecuteAt(runAddress, opcode, cpidrPrefix);

        assertEquals(Add(runAddress, (short) 2), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_BC_is_0_but_Z_is_not_0(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var data1 = Fixture.create(Byte.TYPE);
        var data2 = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = data1;
        Registers.setA(data2);
        Registers.setHL(ToShort(dataAddress));
        Registers.setBC((short) 1);

        ExecuteAt(runAddress, opcode, cpidrPrefix);

        assertEquals(Add(runAddress, (short) 2), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_do_not_decrease_PC_if_BC_is_not_0_and_Z_is_0(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var data1 = Fixture.create(Byte.TYPE);
        var data2 = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = data1;
        Registers.setA(data2);
        Registers.setHL(ToShort(dataAddress));
        Registers.setBC((short) 1000);

        ExecuteAt(runAddress, opcode, cpidrPrefix);

        assertEquals(runAddress, Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_return_proper_T_states_depending_on_PC_being_decreased_or_not(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = Fixture.create(Short.TYPE);
        var runAddress = Fixture.create(Short.TYPE);
        var data1 = Fixture.create(Byte.TYPE);
        var data2 = Fixture.create(Byte.TYPE);

        ProcessorAgent.getMemory()[dataAddress] = data1;
        Registers.setA(data2);
        Registers.setHL(ToShort(dataAddress));
        Registers.setBC((short) 2);

        var states = ExecuteAt(runAddress, opcode, cpidrPrefix);
        assertEquals(21, states);

        Registers.setHL(ToShort(dataAddress));
        states = ExecuteAt(runAddress, opcode, cpidrPrefix);
        assertEquals(16, states);
    }

    protected @Override int Execute(byte opcode, Byte prefix/*= null*/, byte... nextFetches) {
        if (Arrays.binarySearch(cpidrOpcodes, opcode) > 0)
            return super.Execute((opcode & 0xff) == 0xFF ? (byte) 0xB9 : opcode, cpidrPrefix, nextFetches);
        else
            return super.Execute(opcode, prefix, nextFetches);
    }
}
