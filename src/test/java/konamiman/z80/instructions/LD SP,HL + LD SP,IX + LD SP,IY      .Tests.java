package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_SP_HL_IX_IY_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_Source() {
        return Stream.of(
                arguments("HL", (byte) 0xF9, null),
                arguments("IX", (byte) 0xF9, (byte) 0xDD),
                arguments("IY", (byte) 0xF9, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_loads_SP_correctly(String reg, byte opcode, Byte prefix) {
        var newSp = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);

        setReg(reg, newSp);
        registers.setSP(oldSP);

        execute(opcode, prefix);

        assertEquals(newSp, this.<Short>getReg(reg));
        assertEquals(newSp, registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_fire_FetchFinished_with_isLdSp_true(String reg, byte opcode, Byte prefix) {
        var eventFired = new AtomicBoolean(false);

        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.isLdSpInstruction());
        });

        execute(opcode, prefix);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_do_not_change_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_return_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 6 : 10, states);
    }
}
