package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.getBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DAA_tests extends InstructionsExecutionTestsBase {

    private static final byte DAA_opcode = 0x27;

    static Stream<Arguments> DAA_cases_Source() {
        return Stream.of(
                //        N  C  H   A   added  C after
                arguments(0, 0, 0, 0x09, 0x00, 0),
                arguments(0, 0, 0, 0x90, 0x00, 0),

                arguments(0, 0, 0, 0x0F, 0x06, 0),
                arguments(0, 0, 0, 0x8A, 0x06, 0),

                arguments(0, 0, 1, 0x03, 0x06, 0),
                arguments(0, 0, 1, 0x90, 0x06, 0),

                arguments(0, 0, 0, 0xA9, 0x60, 1),
                arguments(0, 0, 0, 0xF0, 0x60, 1),

                arguments(0, 0, 0, 0x9F, 0x66, 1),
                arguments(0, 0, 0, 0xFA, 0x66, 1),

                arguments(0, 0, 1, 0xA3, 0x66, 1),
                arguments(0, 0, 1, 0xF0, 0x66, 1),

                arguments(0, 1, 0, 0x09, 0x60, 1),
                arguments(0, 1, 0, 0x20, 0x60, 1),

                arguments(0, 1, 0, 0x0F, 0x66, 1),
                arguments(0, 1, 0, 0x2A, 0x66, 1),

                arguments(0, 1, 1, 0x03, 0x66, 1),
                arguments(0, 1, 1, 0x30, 0x66, 1),

                arguments(1, 0, 0, 0x09, 0x00, 0),
                arguments(1, 0, 0, 0x90, 0x00, 0),

                arguments(1, 0, 1, 0x0F, 0xFA, 0),
                arguments(1, 0, 1, 0x86, 0xFA, 0),

                arguments(1, 1, 0, 0x79, 0xA0, 1),
                arguments(1, 1, 0, 0xF0, 0xA0, 1),

                arguments(1, 1, 1, 0x6F, 0x9A, 1),
                arguments(1, 1, 1, 0x76, 0x9A, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_A_and_CF_correctly_based_on_input(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(add((byte) inputA, addedValue), registers.getA());
        assertEquals(Bit.of(outputC), registers.getCF());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_returns_proper_T_states(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        var states = execute(DAA_opcode, null);
        assertEquals(4, states);
    }

    @Test
    public void DAA_covers_all_possible_combinations_of_flags_and_A() {
        for (var flagN = 0; flagN <= 1; flagN++)
            for (var flagC = 0; flagC <= 1; flagC++)
                for (var flagH = 0; flagH <= 1; flagH++)
                    for (var valueOfA = 0; valueOfA <= 255; valueOfA++) {
                        setup(flagN, flagC, flagH, valueOfA);
                        execute(DAA_opcode, null);
                    }
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_PF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(parity[registers.getA() & 0xff], registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_SF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(getBit(registers.getA(), 7), registers.getSF());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_ZF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(registers.getA() == 0 ? 1 : 0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_does_not_modify_NF(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        assertDoesNotChangeFlags(DAA_opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_sets_bits_3_and_5_from_of_result(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        setup(inputNF, inputCF, inputHF, inputA);

        execute(DAA_opcode, null);

        assertEquals(registers.getFlag3(), getBit(registers.getA(), 3));
        assertEquals(registers.getFlag5(), getBit(registers.getA(), 5));
    }

    private void setup(int inputNF, int inputCF, int inputHF, int inputA) {
        registers.setNF(Bit.of(inputNF));
        registers.setCF(Bit.of(inputCF));
        registers.setHF(Bit.of(inputHF));
        registers.setA((byte) inputA);
    }
}
