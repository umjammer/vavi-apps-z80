package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class INC_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> INC_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x03, null),
                arguments("DE", (byte) 0x13, null),
                arguments("HL", (byte) 0x23, null),
                arguments("SP", (byte) 0x33, null),
                arguments("IX", (byte) 0x23, (byte) 0xDD),
                arguments("IY", (byte) 0x23, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("INC_rr_Source")
    public void INC_rr_increases_register(String reg, byte opcode, Byte prefix) {
        setReg(reg, (short) 0xFFFF);
        execute(opcode, prefix);
        assertEquals((byte) 0, this.<Short>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("INC_rr_Source")
    public void INC_rr_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("INC_rr_Source")
    public void INC_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 10, 6), states);
    }
}

