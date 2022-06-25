package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.inc;
import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
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
    }

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_loads_register_with_value_and_increases_SP(String reg, byte opcode, Byte prefix) {
        var instructionAddress = fixture.create(Short.TYPE);
        var value = fixture.create(Short.TYPE);
        var oldSP = fixture.create(Short.TYPE);

        registers.setSP(oldSP);
        setMemoryContentsAt(oldSP, getLowByte(value));
        setMemoryContentsAt(inc(oldSP), getHighByte(value));

        executeAt(instructionAddress, opcode, prefix);

        assertEquals(value, this.<Short>getReg(reg));
        assertEquals(NumberUtils.add(oldSP,2), registers.getSP());
    }

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_do_not_modify_flags_unless_AF_is_popped(String reg, byte opcode, Byte prefix) {
        if (!reg.equals("AF"))
            assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("POP_rr_Source")
    public void POP_rr_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.startsWith("I") ? 14 : 10, states);
    }
}
