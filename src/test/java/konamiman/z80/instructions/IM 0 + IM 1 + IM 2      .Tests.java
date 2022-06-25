package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class IM_n_tests extends InstructionsExecutionTestsBase {

    private static final byte prefix = (byte) 0xED;

    static Stream<Arguments> IM_n_Source() {
        return Stream.of(
                arguments((byte) 0, (byte) 0x46),
                arguments((byte) 1, (byte) 0x56),
                arguments((byte) 2, (byte) 0x5E)
        );
    }

    @ParameterizedTest
    @MethodSource("IM_n_Source")
    public void IM_n_changes_interrupt_mode_appropriately(byte newMode, byte opcode) {
        var oldMode = (byte) ((newMode + 1) % 3);

        sut.getProcessorAgent().setInterruptMode2(oldMode);

        execute(opcode, prefix);

        assertEquals(newMode, ((FakeProcessorAgent) sut.getProcessorAgent()).getCurrentInterruptMode());
    }

    @ParameterizedTest
    @MethodSource("IM_n_Source")
    public void IM_n_does_not_modify_flags(byte newMode, byte opcode) {
        assertDoesNotChangeFlags(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("IM_n_Source")
    public void IM_n_returns_proper_T_states(byte newMode, byte opcode) {
        var states = execute(opcode, prefix);
        assertEquals(8, states);
    }
}

