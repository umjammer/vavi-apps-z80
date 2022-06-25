package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class CALL_and_CALL_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> CALL_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC4, 0),
                arguments("C", (byte) 0xD4, 0),
                arguments("P", (byte) 0xE4, 0),
                arguments("S", (byte) 0xF4, 0),
                arguments("Z", (byte) 0xCC, 1),
                arguments("C", (byte) 0xDC, 1),
                arguments("P", (byte) 0xEC, 1),
                arguments("S", (byte) 0xFC, 1)
        );
    }

    static Stream<Arguments> CALL_Source() {
        return Stream.of(
                arguments(null, (byte) 0xCD, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        executeAt(instructionAddress, opcode, null);

        assertEquals(add(instructionAddress, 3), registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_cc_pushes_SP_and_jumps_to_proper_address_if_flag_is_set_CALL_jumps_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);
        var callAddress = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);
        registers.setSP(oldSP);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, getLowByte(callAddress), getHighByte(callAddress));

        assertEquals(callAddress, registers.getPC());
        assertEquals(NumberUtils.sub(oldSP, 2), registers.getSP());
        assertEquals(add(instructionAddress, 3), readShortFromMemory(registers.getSP()));
    }

    private void setFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) setFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        var states = execute(opcode, null);

        assertEquals(17, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        registers.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        var value = registers.getF();

        execute(opcode, null);

        assertEquals(value, registers.getF());
    }
}
