package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.inc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RST_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> RST_Source() {
        return Stream.of(
                arguments(0x00, (byte) 0xC7),
                arguments(0x08, (byte) 0xCF),
                arguments(0x10, (byte) 0xD7),
                arguments(0x18, (byte) 0xDF),
                arguments(0x20, (byte) 0xE7),
                arguments(0x28, (byte) 0xEF),
                arguments(0x30, (byte) 0xF7),
                arguments(0x38, (byte) 0xFF)
        );
    }

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_pushes_SP_and_jumps_to_proper_address(int address, byte opcode) {
        var instructionAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);
        registers.setSP(oldSP);

        executeAt(instructionAddress, opcode, null);

        assertEquals((short) address, registers.getPC());
        assertEquals(NumberUtils.sub(oldSP, 2), registers.getSP());
        assertEquals(inc(instructionAddress), readShortFromMemory(registers.getSP()));
    }

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_return_proper_T_states(int address, byte opcode) {
        var states = execute(opcode, null);

        assertEquals(11, states);
    }

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_do_not_modify_flags(int address, byte opcode) {
        assertDoesNotChangeFlags(opcode, null);
    }
}
