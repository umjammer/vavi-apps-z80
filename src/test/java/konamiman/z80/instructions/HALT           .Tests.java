package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HALT_tests extends InstructionsExecutionTestsBase {

    private static final byte HALT_opcode = 0x76;

    @Test
    public void HALT_fires_fetch_finished_with_isHalt_set() {
        var eventFired = new AtomicBoolean(false);

        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.isHaltInstruction());
        });

        execute(HALT_opcode, null);

        assertTrue(eventFired.get());
    }

    @Test
    public void HALT_does_not_modify_flags() {
        assertDoesNotChangeFlags(HALT_opcode, null);
    }

    @Test
    public void HALT_returns_proper_T_states() {
        var states = execute(HALT_opcode, null);
        assertEquals(4, states);
    }
}
