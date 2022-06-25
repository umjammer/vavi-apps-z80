package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_arr_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_rr_r_Source() {
        return Stream.of(
                arguments("BC", "A", (byte) 0x02),
                arguments("DE", "A", (byte) 0x12),
                arguments("HL", "A", (byte) 0x77),
                arguments("HL", "B", (byte) 0x70),
                arguments("HL", "C", (byte) 0x71),
                arguments("HL", "D", (byte) 0x72),
                arguments("HL", "E", (byte) 0x73),
                arguments("HL", "H", (byte) 0x74),
                arguments("HL", "L", (byte) 0x75)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_arr_r_loads_value_in_memory(String destPointerReg, String srcReg, byte opcode) {
        var isHorL = (srcReg.equals("H") || srcReg.equals("L"));

        var address = fixture.create(Short.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        setReg(destPointerReg, address);
        processorAgent.writeToMemory(address, oldValue);
        if (!isHorL)
            setReg(srcReg, newValue);

        sut.execute(opcode);

        var expected = isHorL ? this.<Byte>getReg(srcReg) : newValue;
        assertEquals(expected, processorAgent.readFromMemory(address));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_do_not_modify_flags(String destPointerReg, String srcReg, byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_r_Source")
    public void LD_rr_r_returns_proper_T_states(String destPointerReg, String srcReg, byte opcode) {
        var states = execute(opcode, null);
        assertEquals(7, states);
    }
}
