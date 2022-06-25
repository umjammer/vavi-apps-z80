package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_I_R_A_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> LD_I_R_A_Source() {
        return Stream.of(
                arguments("I", (byte) 0x47),
                arguments("R", (byte) 0x4F)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_I_R_A_Source")
    public void LD_I_R_A_loads_value_correctly(String reg, byte opcode) {
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);
        setReg(reg, oldValue);
        registers.setA(newValue);

        execute(opcode, prefix);

        assertEquals(newValue, this.<Byte>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_I_R_A_Source")
    public void LD_I_R_A_does_not_modify_flags(String reg, byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_I_R_A_Source")
    public void LD_I_R_A_returns_proper_T_states(String reg, byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(9, states);
    }
}
