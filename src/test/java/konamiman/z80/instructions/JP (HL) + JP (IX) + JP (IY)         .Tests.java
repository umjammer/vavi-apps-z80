package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JP_aHL_IX_IY_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> JP_Source() {
        return Stream.of(
            arguments("HL", (byte) 0xE9, null),
            arguments("IX", (byte) 0xE9, (byte) 0xDD),
            arguments("IY", (byte) 0xE9, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_jump_to_address_contained_in_HL(String reg, byte opcode, Byte prefix) {
        var instructionAddress = fixture.create(Short.TYPE);
        var jumpAddress = fixture.create(Short.TYPE);

        setReg(reg, jumpAddress);
        executeAt(instructionAddress, opcode, prefix);

        assertEquals(jumpAddress, registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_do_not_change_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("JP_Source")
    public void JP_aHL_IX_IY_return_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 4 : 8, states);
    }
}
