package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class JR_and_JR_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> jr_cc_Source() {
        return Stream.of(
                arguments("Z", (byte) 0x20, 0),
                arguments("C", (byte) 0x30, 0),
                arguments("Z", (byte) 0x28, 1),
                arguments("C", (byte) 0x38, 1),
                arguments("A", (byte) 0xBF, 0, null)
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
        var instructionAddress = Fixture.create(Short.TYPE);

        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        ExecuteAt(instructionAddress, opcode, null, /*nextFetches:*/ Fixture.create(Byte.TYPE));

        assertEquals(Add(instructionAddress, (short) 2), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("jr_cc_Source")
    public void JR_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = Execute(opcode, null);

        assertEquals(7, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_cc_jumps_to_proper_address_if_flag_is_set_JR_jumps_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = Fixture.create(Short.TYPE);

        SetFlagIfNotNull(flagName, flagValue);
        ExecuteAt(instructionAddress, opcode, null, /*nextFetches:*/ (byte) 0x7F);
        assertEquals(Add(instructionAddress, (short) 129), Registers.getPC());

        SetFlagIfNotNull(flagName, flagValue);
        ExecuteAt(instructionAddress, opcode, null, /*nextFetches:*/ (byte) 0x80);
        assertEquals(Sub(instructionAddress, (short) 126), Registers.getPC());
    }

    private void SetFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) SetFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_returns_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlagIfNotNull(flagName, flagValue);
        var states = Execute(opcode, null);

        assertEquals(12, states);
    }

    @ParameterizedTest
    @MethodSource({"jr_cc_Source", "jr_Source"})
    public void JR_and_JR_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        Registers.setF(Fixture.create(Byte.TYPE));
        SetFlagIfNotNull(flagName, flagValue);
        var value = Registers.getF();

        Execute(opcode, null);

        assertEquals(value, Registers.getF());
    }
}
