package konamiman.InstructionsExecution;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class RETN_tests extends InstructionsExecutionTestsBase {

    private static final byte opcode = 0x45;
    private static final byte prefix = (byte) 0xED;

    @Test
    public void RETN_returns_to_proper_address() {
        var instructionAddress = Fixture.create(Short.TYPE);
        var returnAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);

        Registers.setSP(oldSP);
        SetMemoryContentsAt(ToUShort(oldSP), GetLowByte(returnAddress));
        SetMemoryContentsAt(Inc(ToUShort(oldSP)), GetHighByte(returnAddress));

        ExecuteAt(instructionAddress, opcode, prefix);

        assertEquals(returnAddress, Registers.getPC());
        assertEquals(Add(oldSP, (short) 2), Registers.getSP());
    }

    @Test
    public void RETN_returns_proper_T_states() {
        var states = Execute(opcode, prefix);

        assertEquals(14, states);
    }

    @Test
    public void RETN_does_not_modify_flags() {
        AssertDoesNotChangeFlags(opcode, prefix);
    }

    @Test
    public void RETN_fires_FetchFinished_with_isRet_true() {
        var eventFired = new AtomicBoolean(false);

        Sut.InstructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue(e.getIsRetInstruction());
        });

        Execute(opcode, prefix);

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
        Registers.setIFF1(Bit.of(initialIFF1));
        Registers.setIFF2(Bit.of(initialIFF2));

        Execute(opcode, prefix);

        assertEquals(initialIFF2, Registers.getIFF2().intValue());
        assertEquals(Registers.getIFF2(), Registers.getIFF1());
    }
}
