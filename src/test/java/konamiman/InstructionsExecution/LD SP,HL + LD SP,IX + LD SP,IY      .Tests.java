package konamiman.InstructionsExecution;

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
        var newSp = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);

        SetReg(reg, newSp);
        Registers.setSP(oldSP);

        Execute(opcode, prefix);

        assertEquals(newSp, this.<Short>GetReg(reg));
        assertEquals(newSp, Registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_fire_FetchFinished_with_isLdSp_true(String reg, byte opcode, Byte prefix) {
        var eventFired = new AtomicBoolean(false);

        Sut.InstructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.getIsLdSpInstruction());
        });

        Execute(opcode, prefix);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_do_not_change_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_SP_HL_IX_IY_return_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 6 : 10, states);
    }
}
