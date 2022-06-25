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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RETN_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x45;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void RETN_returns_to_proper_address() {
        var instructionAddress = fixture.create(Short.TYPE);
        var returnAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);

        registers.setSP(oldSP);
        setMemoryContentsAt(oldSP, getLowByte(returnAddress));
        setMemoryContentsAt(inc(oldSP), getHighByte(returnAddress));

        executeAt(instructionAddress, opcode, prefix);

        assertEquals(returnAddress, registers.getPC());
        assertEquals(add(oldSP, 2), registers.getSP());
    }

    @Test
    public void RETN_returns_proper_T_states() {
        var states = execute(opcode, prefix);

        assertEquals(14, states);
    }

    @Test
    public void RETN_does_not_modify_flags() {
        assertDoesNotChangeFlags(opcode, prefix);
    }

    @Test
    public void RETN_fires_FetchFinished_with_isRet_true() {
        var eventFired = new AtomicBoolean(false);

        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.isRetInstruction());
        });

        execute(opcode, prefix);

        assertTrue(eventFired.get());
    }

    static Stream<Arguments> source() {
        return Stream.of(
                arguments(0, 1),
                arguments(1, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("source")
    public void RETN_copies_IFF2_to_IFF1(int initialIFF1, int initialIFF2) {
        registers.setIFF1(Bit.of(initialIFF1));
        registers.setIFF2(Bit.of(initialIFF2));

        execute(opcode, prefix);

        assertEquals(initialIFF2, registers.getIFF2().intValue());
        assertEquals(registers.getIFF2(), registers.getIFF1());
    }
}
