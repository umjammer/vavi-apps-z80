package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        var address = fixture.create(Short.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        setReg(srcPointerReg, address);
        processorAgent.writeToMemory(address, newValue);
        if (!isHorL)
            setReg(destReg, oldValue);

        sut.execute(opcode);

        assertEquals(newValue, this.<Byte>getReg(destReg));
    }

    @ParameterizedTest
    @MethodSource("LD_r_rr_Source")
    public void LD_r_rr_do_not_modify_flags(String destPointerReg, String srcReg, byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_r_rr_Source")
    public void LD_r_rr_returns_proper_T_states(String destPointerReg, String srcReg, byte opcode) {
        var states = execute(opcode, null);
        assertEquals(7, states);
    }
}
