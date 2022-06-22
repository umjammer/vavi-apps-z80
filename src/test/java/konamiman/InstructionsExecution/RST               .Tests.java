package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
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
    };

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_pushes_SP_and_jumps_to_proper_address(int address, byte opcode) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);
        Registers.setSP(oldSP);

        ExecuteAt(instructionAddress, opcode, null);

        assertEquals((short) address, Registers.getPC());
        assertEquals(Sub(oldSP, (short) 2), Registers.getSP());
        assertEquals(ToShort(Inc(instructionAddress)), ToUShort(ReadShortFromMemory(Registers.getSP())));
    }

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_return_proper_T_states(int address, byte opcode) {
        var states = Execute(opcode, null);

        assertEquals(11, states);
    }

    @ParameterizedTest
    @MethodSource("RST_Source")
    public void RST_do_not_modify_flags(int address, byte opcode) {
        AssertDoesNotChangeFlags(opcode, null);
    }
}
