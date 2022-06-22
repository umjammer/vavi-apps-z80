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
    };

    static Stream<Arguments> RET_Source() {
        return Stream.of(
                arguments(null, (byte) 0xC9, 0)
        );
    };

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_does_not_return_if_flag_not_set(String flagName, byte opcode, int flagValue) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var oldSP = Registers.getSP();

        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        ExecuteAt(instructionAddress, opcode, null);

        assertEquals(Inc(instructionAddress), Registers.getPC());
        assertEquals(oldSP, Registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("RET_cc_Source")
    public void RET_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = Execute(opcode, null);

        assertEquals(5, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_cc_returns_to_proper_address_if_flag_is_set_RET_return_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var returnAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);

        Registers.setSP(oldSP);
        SetMemoryContentsAt(ToUShort(oldSP), GetLowByte(returnAddress));
        SetMemoryContentsAt(Inc(ToUShort(oldSP)), GetHighByte(returnAddress));

        SetFlagIfNotNull(flagName, flagValue);
        ExecuteAt(instructionAddress, opcode, null);

        assertEquals(returnAddress, Registers.getPC());
        assertEquals(Add(oldSP, (short) 2), Registers.getSP());
    }

    private void SetFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) SetFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlagIfNotNull(flagName, flagValue);
        var states = Execute(opcode, null);

        assertEquals(flagName == null ? 10 : 11, states);
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_and_RET_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        Registers.setF(Fixture.create(Byte.TYPE));
        SetFlagIfNotNull(flagName, flagValue);
        var value = Registers.getF();

        Execute(opcode, null);

        assertEquals(value, Registers.getF());
    }

    @ParameterizedTest
    @MethodSource({"RET_cc_Source", "RET_Source"})
    public void RET_fires_FetchFinished_with_isRet_true_if_flag_is_set(String flagName, byte opcode, int flagValue) {
        var eventFired = new AtomicBoolean(false);

        Sut.getProcessorAgent().getRegisters().setF((byte) 255);
        Sut.InstructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            if ((opcode & 0x0F) == 0)
                assertFalse(e.getIsRetInstruction());
            else
                assertTrue(e.getIsRetInstruction());
        });

        Execute(opcode, null);

        assertTrue(eventFired.get());
    }

    @Test
    public void RETI_returns_to_pushed_address() {
        var instructionAddress = Fixture.create(Short.TYPE);
        var returnAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);

        Registers.setSP(oldSP);
        SetMemoryContentsAt(ToUShort(oldSP), GetLowByte(returnAddress));
        SetMemoryContentsAt(Inc(ToUShort(oldSP)), GetHighByte(returnAddress));

        ExecuteAt(instructionAddress, (byte) RETI_opcode, (byte) RETI_prefix);

        assertEquals(returnAddress, Registers.getPC());
        assertEquals(Add(oldSP, (short) 2), Registers.getSP());
    }

    @Test
    public void RETI_returns_proper_T_states() {
        var states = Execute((byte) RETI_opcode, (byte) RETI_prefix);
        assertEquals(14, states);
    }
}
