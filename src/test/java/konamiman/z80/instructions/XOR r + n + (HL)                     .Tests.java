package konamiman.z80.instructions;

import java.util.ArrayList;
import java.util.stream.Stream;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class XOR_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> XOR_r_Source() {
        var combinations = new ArrayList<Arguments>();

        final var registers = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "n", "IXH", "IXL", "IYH", "IYL", "(IX+n)", "(IY+n)"};
        for (var src = 0; src < registers.length; src++) {
            var reg = registers[src];
            var i = new int[] {src};
            Byte[] prefix = new Byte[1];

            modifyTestCaseCreationForIndexRegs(reg, /* ref */i, /* out */prefix);

            var opcode = (byte) (i[0] == 7 ? 0xEE : (i[0] | 0xA8));
            combinations.add(arguments(reg, opcode, prefix[0]));
        }

        return combinations.stream();
    }

    static Stream<Arguments> XOR_A_Source() {
        return Stream.of(
                arguments("A", (byte) 0xAF, null)
        );
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_xors_both_registers(String src, byte opcode, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var valueToXor = fixture.create(Byte.TYPE);

        setup(src, oldValue, valueToXor);
        execute(opcode, prefix);

        assertEquals(oldValue ^ valueToXor, registers.getA());
    }

    @ParameterizedTest
    @MethodSource("XOR_A_Source")
    public void XOR_A_resets_A(String src, byte opcode, Byte prefix) {
        var value = fixture.create(Byte.TYPE);

        registers.setA(value);
        execute(opcode, prefix);

        assertEquals(0, registers.getA());
    }

    private void setup(String src, byte oldValue, byte valueToXor) {
        registers.setA(oldValue);

        if (src.equals("n")) {
            setMemoryContentsAt((short) 1, valueToXor);
        } else if (src.equals("(HL)")) {
            var address = fixture.create(Short.TYPE);
            processorAgent.writeToMemory(address, valueToXor);
            registers.setHL(address);
        } else if (src.startsWith("(I")) {
            var address = fixture.create(Short.TYPE);
            var offset = fixture.create(Byte.TYPE);
            var realAddress = add(address, offset);
            processorAgent.writeToMemory(realAddress, valueToXor);
            setMemoryContentsAt((short) 2, offset);
            setReg(src.substring(1, 1 + 2), address);
        } else if (!src.equals("A")) {
            setReg(src, valueToXor);
        }
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_SF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0x00, prefix);
        assertEquals(1, registers.getSF().intValue());

        executeCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(1, registers.getSF().intValue());

        executeCase(src, opcode, 0xFF, 0xF0, prefix);
        assertEquals(0, registers.getSF().intValue());
    }

    private void executeCase(String src, byte opcode, int oldValue, int valueToAnd, Byte prefix) {
        setup(src, (byte) oldValue, (byte) valueToAnd);
        execute(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_ZF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0x00, prefix);
        assertEquals(0, registers.getZF().intValue());

        executeCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(0, registers.getZF().intValue());

        executeCase(src, opcode, 0xFF, 0xFF, prefix);
        assertEquals(1, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_PF_appropriately(String src, byte opcode, Byte prefix) {
        executeCase(src, opcode, 0xFF, 0x7E, prefix);
        assertEquals(1, registers.getPF().intValue());

        executeCase(src, opcode, 0xFF, 0x7F, prefix);
        assertEquals(0, registers.getPF().intValue());

        executeCase(src, opcode, 0xFF, 0x80, prefix);
        assertEquals(0, registers.getPF().intValue());

        executeCase(src, opcode, 0xFF, 0x81, prefix);
        assertEquals(1, registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"XOR_r_Source", "XOR_A_Source"})
    public void XOR_r_resets_NF_CF_HF(String src, byte opcode, Byte prefix) {
        assertResetsFlags(opcode, null, "N", "C", "H");
    }

    @ParameterizedTest
    @MethodSource("XOR_r_Source")
    public void XOR_r_sets_bits_3_and_5_from_result(String src, byte opcode, Byte prefix) {
        var value = withBit(withBit((byte) 0, 3, 1), 5, 0);
        setup(src, value, (byte) 0);
        execute(opcode, prefix);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        value = withBit(withBit((byte) 0, 3, 0), 5, 1);
        setup(src, value, (byte) 0);
        execute(opcode, prefix);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"XOR_r_Source", "XOR_A_Source"})
    public void XOR_r_returns_proper_T_states(String src, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(
                (src.equals("(HL)") || src.equals("n")) ? 7 :
                        src.startsWith("I") ? 8 :
                                src.startsWith(("(I")) ? 19 :
                                        4, states);
    }
}
