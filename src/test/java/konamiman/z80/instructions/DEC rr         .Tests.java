package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class DEC_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> DEC_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x0B, null),
                arguments("DE", (byte) 0x1B, null),
                arguments("HL", (byte) 0x2B, null),
                arguments("SP", (byte) 0x3B, null),
                arguments("IX", (byte) 0x2B, (byte) 0xDD),
                arguments("IY", (byte) 0x2B, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("DEC_rr_Source")
    public void DEC_rr_decreases_register(String reg, byte opcode, Byte prefix) {
        setReg(reg, (short) 0);
        execute(opcode, prefix);
        assertEquals((short) 0xFFFF, this.<Short>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("DEC_rr_Source")
    public void DEC_rr_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("DEC_rr_Source")
    public void DEC_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(ifIndexRegister(reg, 10, 6), states);
    }
}

