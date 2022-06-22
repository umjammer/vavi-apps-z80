package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
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
        var instructionAddress = Fixture.create(Short.TYPE);

        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        ExecuteAt(instructionAddress, opcode, null);

        assertEquals(Add(instructionAddress, (short) 3), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("JP_cc_Source")
    public void JP_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = Execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_cc_jumps_to_proper_address_if_flag_is_set_JP_jumps_always(String flagName, byte opcode, int flagValue) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var jumpAddress = Fixture.create(Short.TYPE);

        SetFlagIfNotNull(flagName, flagValue);
        ExecuteAt(instructionAddress, opcode, null, /*nextFetches:*/ GetLowByte(jumpAddress), GetHighByte(jumpAddress));

        assertEquals(jumpAddress, Registers.getPC());
    }

    private void SetFlagIfNotNull(String flagName, int flagValue) {
        if (flagName != null) SetFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue) {
        SetFlagIfNotNull(flagName, flagValue);
        var states = Execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"JP_cc_Source", "JP_Source"})
    public void JP_and_JP_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue) {
        Registers.setF(Fixture.create(Byte.TYPE));
        SetFlagIfNotNull(flagName, flagValue);
        var value = Registers.getF();

        Execute(opcode, null);

        assertEquals(value, Registers.getF());
    }
}
