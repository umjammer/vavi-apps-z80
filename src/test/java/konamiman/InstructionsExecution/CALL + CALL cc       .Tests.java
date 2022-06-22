package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Sub;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class CALL_and_CALL_cc_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> CALL_cc_Source() {
        return Stream.of(
            arguments("Z", (byte)0xC4, 0),
            arguments("C", (byte)0xD4, 0),
            arguments("P", (byte)0xE4, 0),
            arguments("S", (byte)0xF4, 0),
            arguments("Z", (byte)0xCC, 1),
            arguments("C", (byte)0xDC, 1),
            arguments("P", (byte)0xEC, 1),
            arguments("S", (byte)0xFC, 1)
        );
    }

    static Stream<Arguments> CALL_Source() {
        return Stream.of(
            arguments(null, (byte)0xCD, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_does_not_jump_if_flag_not_set(String flagName, byte opcode, int flagValue)
    {
        var instructionAddress = Fixture.create(Short.TYPE);

        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        ExecuteAt(instructionAddress, opcode, null);

        assertEquals(Add(instructionAddress, (short) 3), Registers.getPC());
    }

    @ParameterizedTest
    @MethodSource("CALL_cc_Source")
    public void CALL_cc_returns_proper_T_states_if_no_jump_is_made(String flagName, byte opcode, int flagValue)
    {
        SetFlag(flagName, Bit.of(flagValue).operatorNOT());
        var states = Execute(opcode, null);

        assertEquals(10, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_cc_pushes_SP_and_jumps_to_proper_address_if_flag_is_set_CALL_jumps_always(String flagName, byte opcode, int flagValue)
    {
        var instructionAddress = Fixture.create(Short.TYPE);
        var callAddress = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);
        Registers.setSP(oldSP);

        SetFlagIfNotNull(flagName, flagValue);
        ExecuteAt(instructionAddress, opcode, null, /*nextFetches:*/ GetLowByte(callAddress), GetHighByte(callAddress));

        assertEquals(callAddress, Registers.getPC());
        assertEquals(Sub(oldSP, (short) 2), Registers.getSP());
        assertEquals(ToShort(Add(instructionAddress, (short) 3)), ToUShort(ReadShortFromMemory(Registers.getSP())));
    }

    private void SetFlagIfNotNull(String flagName, int flagValue)
    {
        if (flagName != null) SetFlag(flagName, Bit.of(flagValue));
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_return_proper_T_states_if_jump_is_made(String flagName, byte opcode, int flagValue)
    {
        SetFlagIfNotNull(flagName, flagValue);
        var states = Execute(opcode, null);

        assertEquals(17, states);
    }

    @ParameterizedTest
    @MethodSource({"CALL_cc_Source", "CALL_Source"})
    public void CALL_and_CALL_cc_do_not_modify_flags(String flagName, byte opcode, int flagValue)
    {
        Registers.setF(Fixture.create(Byte.TYPE));
        SetFlagIfNotNull(flagName, flagValue);
        var value = Registers.getF();

        Execute(opcode, null);

        assertEquals(value, Registers.getF());
    }
}
