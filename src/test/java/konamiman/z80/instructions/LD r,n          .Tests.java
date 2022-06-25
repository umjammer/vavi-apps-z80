package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_r_n_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_r_n_Source() {
        return Stream.of(
                arguments("A", (byte) 0x3E, null),
                arguments("B", (byte) 0x06, null),
                arguments("C", (byte) 0x0E, null),
                arguments("D", (byte) 0x16, null),
                arguments("E", (byte) 0x1E, null),
                arguments("H", (byte) 0x26, null),
                arguments("L", (byte) 0x2E, null),
                arguments("IXH", (byte) 0x26, (byte) 0xDD),
                arguments("IXL", (byte) 0x2E, (byte) 0xDD),
                arguments("IYH", (byte) 0x26, (byte) 0xFD),
                arguments("IYL", (byte) 0x2E, (byte) 0xFD)
        );
    }

    public static Object[] LD_r_n_Source = {
    };

    @ParameterizedTest
    @MethodSource("LD_r_n_Source")
    public void LD_r_n_loads_register_with_value(String reg, byte opcode, Byte prefix) {
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);

        setReg(reg, oldValue);

        execute(opcode, prefix, newValue);

        assertEquals(newValue, this.<Byte>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_r_n_Source")
    public void LD_r_n_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_r_n_Source")
    public void LD_r_n_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 11, 7), states);
    }
}
