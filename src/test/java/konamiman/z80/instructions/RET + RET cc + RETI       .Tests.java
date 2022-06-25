package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static konamiman.z80.utils.NumberUtils.inc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RET_and_RET_cc_tests extends InstructionsExecutionTestsBase {

    private static final int RETI_opcode = 0x4D;
    private static final int RETI_prefix = 0xED;

    static Stream<Arguments> RET_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC0, 0),
                arguments("C", (byte) 0xD0, 0),
                arguments("P", (byte) 0xE0, 0),
                arguments("S", (byte) 0xF0, 0),
                arguments("Z", (byte) 0xC8, 1),
                arguments("C", (byte) 0xD8, 1),
                arguments("P", (byte) 0xE8, 1),
                arguments("S", (byte) 0xF8, 1)
        );
    }

    static Stream<Arguments> RET_Source() {
        return Stream.of(
                arguments(null, (byte) 0xC9, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_does_not_return_if_flag_not_set(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);
        var oldSP = registers.getSP();

        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        executeAt(instructionAddress, opcode, null);

        assertEquals(inc(instructionAddress), registers.getPC());
        assertEquals(oldSP, registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = execute(opcode, null);

        assertEquals(5, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_cc_returns_to_proper_address_if_flag_is_set_RET_return_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);
        var returnAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);

        registers.setSP(oldSP);
        setMemoryContentsAt(oldSP, getLowByte(returnAddress));
        setMemoryContentsAt(inc(oldSP), getHighByte(returnAddress));

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null);

        assertEquals(returnAddress, registers.getPC());
        assertEquals(add(oldSP, 2), registers.getSP());
    }

    private void setFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) setFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        var states = execute(opcode, null);

        assertEquals(flagName == null ? 10 : 11, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        registers.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        var value = registers.getF();

        execute(opcode, null);

        assertEquals(value, registers.getF());
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_fires_FetchFinished_with_isRet_true_if_flag_is_set(String flagName, byte opcode, int flagValue) {
        var eventFired = new AtomicBoolean(false);

        sut.getProcessorAgent().getRegisters().setF((byte) 255);
        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            if ((opcode & 0x0F) == 0)
                assertFalse(e.isRetInstruction());
            else
                assertTrue(e.isRetInstruction());
        });

        execute(opcode, null);

        assertTrue(eventFired.get());
    }

    @Test
    public void RETI_returns_to_pushed_address() {
        var instructionAddress = fixture.create(Short.TYPE);
        var returnAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);

        registers.setSP(oldSP);
        setMemoryContentsAt(oldSP, getLowByte(returnAddress));
        setMemoryContentsAt(inc(oldSP), getHighByte(returnAddress));

        executeAt(instructionAddress, (byte) RETI_opcode, (byte) RETI_prefix);

        assertEquals(returnAddress, registers.getPC());
        assertEquals(add(oldSP, 2), registers.getSP());
    }

    @Test
    public void RETI_returns_proper_T_states() {
        var states = execute((byte) RETI_opcode, (byte) RETI_prefix);
        assertEquals(14, states);
    }
}
