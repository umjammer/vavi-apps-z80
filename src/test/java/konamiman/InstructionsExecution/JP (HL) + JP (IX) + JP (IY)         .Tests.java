package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JP_aHL_IX_IY_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> JP_Source() {
        return Stream.of(
            arguments("HL", (byte) 0xE9, null),
            arguments("IX", (byte) 0xE9, (byte) 0xDD),
            arguments("IY", (byte) 0xE9, (byte) 0xFD)
        );
    };

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_jump_to_address_contained_in_HL(String reg, byte opcode, Byte prefix) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var jumpAddress = Fixture.create(Short.TYPE);

        SetReg(reg, jumpAddress);
        ExecuteAt(instructionAddress, opcode, prefix);

        assertEquals(ToUShort(jumpAddress), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_do_not_change_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_return_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 4 : 8, states);
    }
}
