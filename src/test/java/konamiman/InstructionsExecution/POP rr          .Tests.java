package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static konamiman.DataTypesAndUtils.NumberUtils.Inc;
import static konamiman.DataTypesAndUtils.NumberUtils.ToUShort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class POP_rr_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> POP_rr_Source() {
        return Stream.of(
                arguments("BC", (byte) 0xC1, null),
                arguments("DE", (byte) 0xD1, null),
                arguments("HL", (byte) 0xE1, null),
                arguments("AF", (byte) 0xF1, null),
                arguments("IX", (byte) 0xE1, (byte) 0xDD),
                arguments("IY", (byte) 0xE1, (byte) 0xFD)
        );
    };

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_loads_register_with_value_and_increases_SP(String reg, byte opcode, Byte prefix) {
        var instructionAddress = Fixture.create(Short.TYPE);
        var value = Fixture.create(Short.TYPE);
        var oldSP = Fixture.create(Short.TYPE);

        Registers.setSP(oldSP);
        SetMemoryContentsAt(ToUShort(oldSP), GetLowByte(value));
        SetMemoryContentsAt(Inc(ToUShort(oldSP)), GetHighByte(value));

        ExecuteAt(instructionAddress, opcode, prefix);

        assertEquals(value, this.<Short>GetReg(reg));
        assertEquals(Add(oldSP, (short) 2), Registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_do_not_modify_flags_unless_AF_is_popped(String reg, byte opcode, Byte prefix) {
        if (!reg.equals("AF"))
            AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(reg.startsWith("I") ? 14 : 10, states);
    }
}
