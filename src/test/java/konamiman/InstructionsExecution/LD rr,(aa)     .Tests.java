package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_rr_aa_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_rr_aa_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x2A, null),
                arguments("DE", (byte) 0x5B, (byte) 0xED),
                arguments("BC", (byte) 0x4B, (byte) 0xED),
                arguments("SP", (byte) 0x7B, (byte) 0xED),
                arguments("IX", (byte) 0x2A, (byte) 0xDD),
                arguments("IY", (byte) 0x2A, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_aa_loads_value_from_memory(String reg, byte opcode, Byte prefix) {
        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Short.TYPE);
        var newValue = Fixture.create(Short.TYPE);

        SetReg(reg, oldValue);
        WriteShortToMemory(address, newValue);

        Execute(opcode, prefix, /*nextFetches:*/ ToByteArray(address));

        assertEquals(newValue, ReadShortFromMemory(address));
        assertEquals(newValue, this.<Short>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_r_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 16 : 20, states);
    }
}
