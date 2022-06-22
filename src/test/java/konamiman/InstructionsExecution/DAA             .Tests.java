package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetBit;
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
        Setup(inputNF, inputCF, inputHF, inputA);

        Execute(DAA_opcode, null);

        assertEquals(Add((byte) inputA, addedValue), Registers.getA());
        assertEquals(Bit.of(outputC), Registers.getCF());
    }


    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_returns_proper_T_states(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        var states = Execute(DAA_opcode, null);
        assertEquals(4, states);
    }

    @Test
    public void DAA_covers_all_possible_combinations_of_flags_and_A() {
        for (var flagN = 0; flagN <= 1; flagN++)
            for (var flagC = 0; flagC <= 1; flagC++)
                for (var flagH = 0; flagH <= 1; flagH++)
                    for (var valueOfA = 0; valueOfA <= 255; valueOfA++) {
                        Setup(flagN, flagC, flagH, valueOfA);
                        Execute(DAA_opcode, null);
                    }
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_PF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        Execute(DAA_opcode, null);

        assertEquals(Parity[Registers.getA()], Registers.getPF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_SF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        Execute(DAA_opcode, null);

        assertEquals(GetBit(Registers.getA(), 7), Registers.getSF());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_generates_ZF_properly(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        Execute(DAA_opcode, null);

        assertEquals(Registers.getA() == 0 ? 1 : 0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_does_not_modify_NF(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        AssertDoesNotChangeFlags(DAA_opcode, null, "N");
    }

    @ParameterizedTest
    @MethodSource("DAA_cases_Source")
    public void DAA_sets_bits_3_and_5_from_of_result(int inputNF, int inputCF, int inputHF, int inputA, int addedValue, int outputC) {
        Setup(inputNF, inputCF, inputHF, inputA);

        Execute(DAA_opcode, null);

        assertEquals(Registers.getFlag3(), GetBit(Registers.getA(), 3));
        assertEquals(Registers.getFlag5(), GetBit(Registers.getA(), 5));
    }

    private void Setup(int inputNF, int inputCF, int inputHF, int inputA) {
        Registers.setNF(Bit.of(inputNF));
        Registers.setCF(Bit.of(inputCF));
        Registers.setHF(Bit.of(inputHF));
        Registers.setA((byte) inputA);
    }
}
