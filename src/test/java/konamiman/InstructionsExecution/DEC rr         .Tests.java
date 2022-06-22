package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
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
        SetReg(reg, (byte) 0);
        Execute(opcode, prefix);
        assertEquals(ToShort(0xFFFF), this.<Short>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource("DEC_rr_Source")
    public void DEC_rr_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("DEC_rr_Source")
    public void DEC_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(IfIndexRegister(reg, 10, /*@else:*/ 6), states);
    }
}

