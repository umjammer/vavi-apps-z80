package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/*partial*/ class LD_r_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_r_rr_Source() {
        return Stream.of(
                arguments("A", "BC", (byte) 0x0A),
                arguments("A", "DE", (byte) 0x1A),
                arguments("A", "HL", (byte) 0x7E),
                arguments("B", "HL", (byte) 0x46),
                arguments("C", "HL", (byte) 0x4E),
                arguments("D", "HL", (byte) 0x56),
                arguments("E", "HL", (byte) 0x5E),
                arguments("H", "HL", (byte) 0x66),
                arguments("L", "HL", (byte) 0x6E)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_r_rr_Source")
    public void LD_arr_r_loads_value_from_memory(String destReg, String srcPointerReg, byte opcode) {
        var isHorL = (destReg.equals("H") || destReg.equals("L"));

        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);

        SetReg(srcPointerReg, ToShort(address));
        ProcessorAgent.getMemory()[address] = newValue;
        if (!isHorL)
            SetReg(destReg, oldValue);

        Sut.execute(opcode);

        assertEquals(newValue, this.<Byte>GetReg(destReg));
    }

    @ParameterizedTest
    @MethodSource("LD_r_rr_Source")
    public void LD_r_rr_do_not_modify_flags(String destPointerReg, String srcReg, byte opcode) {
        AssertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_r_rr_Source")
    public void LD_r_rr_returns_proper_T_states(String destPointerReg, String srcReg, byte opcode) {
        var states = Execute(opcode, null);
        assertEquals(7, states);
    }
}
