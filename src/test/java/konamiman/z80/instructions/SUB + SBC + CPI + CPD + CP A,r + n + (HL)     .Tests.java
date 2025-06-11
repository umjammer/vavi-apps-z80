package konamiman.z80.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavi.util.Debug;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.dec;
import static konamiman.z80.utils.NumberUtils.getBit;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.sub;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class SUB_SBC_CPI_CPD_CP_r_tests extends InstructionsExecutionTestsBase {

    private static final byte cpidrPrefix = (byte) 0xED;
    /** should be sorted (used for binary search) */
    private static final byte[] cpidrOpcodes = new byte[] {(byte) 0xA1, (byte) 0xA9, (byte) 0xB1, (byte) 0xFF};

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

    public static final List<Arguments> SUB_SBC_A_r;
    public static final List<Arguments> CP_r;
    public static final List<Arguments> CPID_R;

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

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

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

        CPID_R = Stream.of(CPI_Source(), CPD_Source(), CPIR_Source(), CPDR_Source())
                .flatMap(Function.identity()).collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source"})
    public void SUB_SBC_A_r_subtracts_both_registers_with_or_without_carry(String src, byte opcode, int cf, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var valueToSubtract = src.equals("A") ? oldValue : fixture.create(Byte.TYPE);

        setup(src, oldValue, valueToSubtract, cf);
        execute(opcode, prefix);

        assertEquals(NumberUtils.sub(oldValue, (valueToSubtract & 0xff) + cf), registers.getA());
    }

    @ParameterizedTest
    @MethodSource({"CP_r_Source", "CP_A_Source", "CPID_R_Source"})
    public void CPs_do_not_change_A(String src, byte opcode, int cf, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var argument = fixture.create(Byte.TYPE);

        setup(src, oldValue, argument, cf);
        execute(opcode, prefix);

        assertEquals(oldValue, registers.getA());
    }

    private void setup(String src, byte oldValue, byte valueToSubtract, int cf /* = 0 */) {
        registers.setA(oldValue);
        registers.setCF(Bit.of(cf));

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToSubtract);
        } else if (src.equals("(HL)") || src.startsWith("CP")) {
            // TODO got error when 1 at "set_HF_appropriately" (CPDR)
            var address = createAddressFixture();
            processorAgent.writeToMemory(address, valueToSubtract);
            registers.setHL(address);
        } else if (src.startsWith("(I")) {
            // TODO got error when 1 at "*" (CPI)
            var address = createAddressFixture();
            var offset = fixture.create(Byte.TYPE);
            var realAddress = add(address, offset);
            processorAgent.writeToMemory(realAddress, valueToSubtract);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToSubtract);
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_SF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        setup(src, (byte) 0x02, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (byte) 0x01, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getSF().intValue());

        setup(src, (byte) 0x00, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());

        setup(src, (byte) 0xFF, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_ZF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        setup(src, (byte) 0x03, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (byte) 0x02, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());

        setup(src, (byte) 0x01, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getZF().intValue());

        setup(src, (byte) 0x00, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_set_HF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        for (byte b : new byte[] {0x11, (byte) 0x81, (byte) 0xF1}) {
            setup(src, b, (byte) 1, 0);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());

            setup(src, (byte) (b - 1), (byte) 1, 0);
            execute(opcode, prefix);
            assertEquals(1, registers.getHF().intValue());

            setup(src, (byte) (b - 2), (byte) 1, 0);
            execute(opcode, prefix);
            assertEquals(0, registers.getHF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_PF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        //http://stackoverflow.com/a/8037485/4574

        testPF(src, opcode, 127, 0, 0, prefix);
        testPF(src, opcode, 127, 1, 0, prefix);
        testPF(src, opcode, 127, 127, 0, prefix);
        testPF(src, opcode, 127, 128, 1, prefix);
        testPF(src, opcode, 127, 129, 1, prefix);
        testPF(src, opcode, 127, 255, 1, prefix);
        testPF(src, opcode, 128, 0, 0, prefix);
        testPF(src, opcode, 128, 1, 1, prefix);
        testPF(src, opcode, 128, 127, 1, prefix);
        testPF(src, opcode, 128, 128, 0, prefix);
        testPF(src, opcode, 128, 129, 0, prefix);
        testPF(src, opcode, 128, 255, 0, prefix);
        testPF(src, opcode, 129, 0, 0, prefix);
        testPF(src, opcode, 129, 1, 0, prefix);
        testPF(src, opcode, 129, 127, 1, prefix);
        testPF(src, opcode, 129, 128, 0, prefix);
        testPF(src, opcode, 129, 129, 0, prefix);
        testPF(src, opcode, 129, 255, 0, prefix);
    }

    void testPF(String src, byte opcode, int oldValue, int subtractedValue, int expectedPF, Byte prefix) {
        setup(src, (byte) oldValue, (byte) subtractedValue, 0);

        execute(opcode, prefix);
        assertEquals(expectedPF, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source", "CPID_R_Source"})
    public void SUB_SBC_CPs_sets_NF(String src, byte opcode, int cf, Byte prefix) {
        assertSetsFlags(opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_sets_CF_appropriately(String src, byte opcode, int cf, Byte prefix) {
        setup(src, (byte) 0x01, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());

        setup(src, (byte) 0x00, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getCF().intValue());

        setup(src, (byte) 0xFF, (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getCF().intValue());
    }

    @ParameterizedTest
    @MethodSource("SUB_SBC_A_r_Source")
    public void SUB_SBC_r_sets_bits_3_and_5_from_result(String src, byte opcode, int cf, Byte prefix) {
        setup(src, (byte) (withBit(withBit((byte) 0, 3, 1), 5, 0) + 1), (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        setup(src, (byte) (withBit(withBit((byte) 0, 3, 0), 5, 1) + 1), (byte) 1, 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"SUB_SBC_A_r_Source", "SUB_SBC_A_A_Source", "CP_r_Source"})
    public void SUB_SBC_CP_r_returns_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPRD_do_not_change_C(String src, byte opcode, int cf, Byte prefix) {
        assertDoesNotChangeFlags(opcode, cpidrPrefix, "C");
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_reset_PF_if_BC_reaches_zero(String src, byte opcode, int cf, Byte prefix) {
        registers.setBC((short) 128);
        for (int i = 0; i <= 256; i++) {
            var oldBC = registers.getBC();
            execute(opcode, prefix);
            assertEquals(dec(oldBC), registers.getBC());
            assertEquals(registers.getBC() != 0, registers.getPF().booleanValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPIR_Source"})
    public void CPI_CPIR_increase_HL(String src, byte opcode, int cf, Byte prefix) {
        var address = fixture.create(Short.TYPE);

        registers.setHL(address);

        execute(opcode, prefix);

        assertEquals(inc(address), registers.getHL());
    }

    @ParameterizedTest
    @MethodSource({"CPD_Source", "CPDR_Source"})
    public void CPD_CPDR_decrease_HL(String src, byte opcode, int cf, Byte prefix) {
        var address = fixture.create(Short.TYPE);

        registers.setHL(address);

        execute(opcode, prefix);

        assertEquals(dec(address), registers.getHL());
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_CPIR_CPDR_decrease_BC(String src, byte opcode, int cf, Byte prefix) {
        var count = fixture.create(Short.TYPE);

        registers.setBC(count);

        execute(opcode, prefix);

        assertEquals(dec(count), registers.getBC());
    }

    @ParameterizedTest
    @MethodSource({"CPI_Source", "CPD_Source"})
    public void CPI_CPD_return_proper_T_states(String src, byte opcode, int cf, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(16, states);
    }

    @ParameterizedTest
    @MethodSource("CPID_R_Source")
    public void CPI_CPD_set_Flag3_from_bit_3_of_A_minus_aHL_minus_HF_and_Flag5_from_bit_1(String src, byte opcode, int cf, Byte prefix) {
        var valueInMemory = fixture.create(Byte.TYPE);
        // TODO got error when 1
        var srcAddress = createAddressFixture();

        for (int i = 0; i < 256; i++) {
            var valueOfA = (byte) i;
            registers.setA(valueOfA);
            registers.setHL(srcAddress);
            processorAgent.writeToMemory(srcAddress, valueInMemory);

            execute(opcode, prefix);

            var expected = getLowByte(sub(sub(valueOfA, valueInMemory & 0xff), registers.getHF().intValue()));
            assertEquals(getBit(expected, 3), registers.getFlag3());
            assertEquals(getBit(expected, 1), registers.getFlag5());
        }
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_Z_is_1_but_BC_is_not_0(String src, byte opcode, int cf, Byte prefix) {
        // TODO got error
        // when 88, 50, 85 at (HL)
        // when 6, 125, 35 at (HL)
        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var data = fixture.create(Byte.TYPE);

Debug.printf("%d, %d, %d", dataAddress, runAddress, data);

        processorAgent.writeToMemory(dataAddress, data);
        registers.setA(data);
        registers.setHL(dataAddress);
        registers.setBC(fixture.create(Short.TYPE));

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(add(runAddress, 2), registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_decrease_PC_if_BC_is_0_but_Z_is_not_0(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var data1 = fixture.create(Byte.TYPE);
        var data2 = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(dataAddress, data1);
        registers.setA(data2);
        registers.setHL(dataAddress);
        registers.setBC((short) 1);

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(add(runAddress, 2), registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_do_not_decrease_PC_if_BC_is_not_0_and_Z_is_0(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var data1 = fixture.create(Byte.TYPE);
        var data2 = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(dataAddress, data1);
        registers.setA(data2);
        registers.setHL(dataAddress);
        registers.setBC((short) 1000);

        executeAt(runAddress, opcode, cpidrPrefix);

        assertEquals(runAddress, registers.getPC());
    }

    @ParameterizedTest
    @MethodSource({"CPIR_Source", "CPDR_Source"})
    public void CPIR_CPDR_return_proper_T_states_depending_on_PC_being_decreased_or_not(String src, byte opcode, int cf, Byte prefix) {
        if ((opcode & 0xff) == 0xFF) opcode = (byte) 0xB9;

        var dataAddress = fixture.create(Short.TYPE);
        var runAddress = fixture.create(Short.TYPE);
        var data1 = fixture.create(Byte.TYPE);
        var data2 = fixture.create(Byte.TYPE);

        processorAgent.writeToMemory(dataAddress, data1);
        registers.setA(data2);
        registers.setHL(dataAddress);
        registers.setBC((short) 2);

        var states = executeAt(runAddress, opcode, cpidrPrefix);
        assertEquals(21, states);

        registers.setHL(dataAddress);
        states = executeAt(runAddress, opcode, cpidrPrefix);
        assertEquals(16, states);
    }

    @Override protected int execute(byte opcode, Byte prefix /* = null */, byte... nextFetches) {
        if (Arrays.binarySearch(cpidrOpcodes, opcode) >= 0)
            return super.execute((opcode & 0xff) == 0xFF ? (byte) 0xB9 : opcode, cpidrPrefix, nextFetches);
        else
            return super.execute(opcode, prefix, nextFetches);
    }
}
