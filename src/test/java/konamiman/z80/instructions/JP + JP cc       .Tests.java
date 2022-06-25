package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JP_and_JP_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> JP_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0xC2, 0),
                arguments("C", (byte) 0xD2, 0),
                arguments("P", (byte) 0xE2, 0),
                arguments("S", (byte) 0xF2, 0),
                arguments("Z", (byte) 0xCA, 1),
                arguments("C", (byte) 0xDA, 1),
                arguments("P", (byte) 0xEA, 1),
                arguments("S", (byte) 0xFA, 1)
        );
    }

    static Stream<Arguments> JP_Source() {
        return Stream.of(
                arguments(null, (byte) 0xC3, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("JP_cc_Source")
    public void JP_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        executeAt(instructionAddress, opcode, null);

        assertEquals(add(instructionAddress, 3), registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("JP_cc_Source")
    public void JP_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_cc_jumps_to_proper_address_if_flag_is_set_JP_jumps_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);
        var jumpAddress = fixture.create(Short.TYPE);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, getLowByte(jumpAddress), getHighByte(jumpAddress));

        assertEquals(jumpAddress, registers.getPC());
    }

    private void setFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) setFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        var states = execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        registers.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        var value = registers.getF();

        execute(opcode, null);

        assertEquals(value, registers.getF());
    }
}
