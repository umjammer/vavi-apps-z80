package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicBoolean;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DI_EI_tests extends InstructionsExecutionTestsBase {

    private static final byte DI_opcode = (byte) 0xF3;
    private static final byte EI_opcode = (byte) 0xFB;

    @Test
    public void DI_resets_IFF() {
        registers.setIFF1(Bit.ON);
        registers.setIFF2(Bit.ON);

        execute(DI_opcode, null);

        assertEquals(0, registers.getIFF1().intValue());
        assertEquals(0, registers.getIFF2().intValue());
    }

    @Test
    public void EI_sets_IFF() {
        registers.setIFF1(Bit.OFF);
        registers.setIFF2(Bit.OFF);

        execute(EI_opcode, null);

        assertEquals(1, registers.getIFF1().intValue());
        assertEquals(1, registers.getIFF2().intValue());
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void EI_fires_FetchFinished_with_isEiOrDi_true(byte opcode) {
        var eventFired = new AtomicBoolean(false);

        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.isEiOrDiInstruction());
        });

        execute(opcode, null);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_do_not_change_flags(byte opcode) {
        assertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_return_proper_T_states(byte opcode) {
        var states = execute(opcode, null);
        assertEquals(4, states);
    }
}
