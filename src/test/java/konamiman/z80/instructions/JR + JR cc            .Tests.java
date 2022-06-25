package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JR_and_JR_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> jr_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0x20, 0),
                arguments("C", (byte) 0x30, 0),
                arguments("Z", (byte) 0x28, 1),
                arguments("C", (byte) 0x38, 1)
        );
    }

    static Stream<Arguments> jr_Source() {
        return Stream.of(
                arguments(null, (byte) 0x18, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("jr_cc_Source")
    public void JR_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);

        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        executeAt(instructionAddress, opcode, null, fixture.create(Byte.TYPE));

        assertEquals(add(instructionAddress, 2), registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("jr_cc_Source")
    public void JR_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = execute(opcode, null);

        assertEquals(7, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_cc_jumps_to_proper_address_if_flag_is_set_JR_jumps_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = fixture.create(Short.TYPE);

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, (byte) 0x7F);
        assertEquals(add(instructionAddress, 129), registers.getPC());

        setFlagIfNotNull(flagName, flagValue);
        executeAt(instructionAddress, opcode, null, (byte) 0x80);
        assertEquals(NumberUtils.sub(instructionAddress, 126), registers.getPC());
    }

    private void setFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) setFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_returns_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        setFlagIfNotNull(flagName, flagValue);
        var states = execute(opcode, null);

        assertEquals(12, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        registers.setF(fixture.create(Byte.TYPE));
        setFlagIfNotNull(flagName, flagValue);
        var value = registers.getF();

        execute(opcode, null);

        assertEquals(value, registers.getF());
    }
}
