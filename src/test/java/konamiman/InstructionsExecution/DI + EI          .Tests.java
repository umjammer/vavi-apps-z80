package konamiman.InstructionsExecution;

import java.util.concurrent.atomic.AtomicBoolean;

import konamiman.DataTypesAndUtils.Bit;
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
        Registers.setIFF1(Bit.ON);
        Registers.setIFF2(Bit.ON);

        Execute(DI_opcode, null);

        assertEquals(0, Registers.getIFF1().intValue());
        assertEquals(0, Registers.getIFF2().intValue());
    }

    @Test
    public void EI_sets_IFF() {
        Registers.setIFF1(Bit.OFF);
        Registers.setIFF2(Bit.OFF);

        Execute(EI_opcode, null);

        assertEquals(1, Registers.getIFF1().intValue());
        assertEquals(1, Registers.getIFF2().intValue());
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void EI_fires_FetchFinished_with_isEiOrDi_true(byte opcode) {
        var eventFired = new AtomicBoolean(false);

        Sut.InstructionFetchFinished().addListener(e ->
        {
            eventFired.set(true);
            assertTrue(e.getIsEiOrDiInstruction());
        });

        Execute(opcode, null);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_do_not_change_flags(byte opcode) {
        AssertNoFlagsAreModified(opcode, null);
    }

    @ParameterizedTest
    @ValueSource(bytes = {EI_opcode, DI_opcode})
    public void DI_EI_return_proper_T_states(byte opcode) {
        var states = Execute(opcode, null);
        assertEquals(4, states);
    }
}
