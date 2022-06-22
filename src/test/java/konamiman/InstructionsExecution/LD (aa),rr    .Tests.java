package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_aa_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_aa_rr_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x22, null),
                arguments("DE", (byte) 0x53, (byte) 0xED),
                arguments("BC", (byte) 0x43, (byte) 0xED),
                arguments("SP", (byte) 0x73, (byte) 0xED),
                arguments("IX", (byte) 0x22, (byte) 0xDD),
                arguments("IY", (byte) 0x22, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_aa_rr_loads_value_in_memory(String reg, byte opcode, Byte prefix) {
        var address = Fixture.create(Short.TYPE);
        var oldValue = Fixture.create(Short.TYPE);
        var newValue = Fixture.create(Short.TYPE);

        SetReg(reg, newValue);
        WriteShortToMemory(address, oldValue);

        Execute(opcode, prefix, /*nextFetches:*/ ToByteArray(address));

        assertEquals(newValue, ReadShortFromMemory(address));
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_rr_r_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_aa_rr_Source")
    public void LD_rr_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 16 : 20, states);
    }
}
