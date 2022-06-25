package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class EX_SP_HL_IX_IY_tests extends InstructionsExecutionTestsBase {

    private static final byte EX_SP_HL_opcode = (byte) 0xE3;

    static Stream<Arguments> EX_Source() {
        return Stream.of(
                arguments("HL", EX_SP_HL_opcode, null),
                arguments("IX", EX_SP_HL_opcode, (byte) 0xDD),
                arguments("IY", EX_SP_HL_opcode, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("EX_Source")
    public void EX_SP_HL_IX_IY_exchanges_reg_and_pushed_value(String reg, byte opcode, Byte prefix) {
        var regValue = fixture.create(Short.TYPE);
        var pushedValue = fixture.create(Short.TYPE);
        var SP = fixture.create(Short.TYPE);

        setReg(reg, regValue);
        registers.setSP(SP);
        writeShortToMemory(SP, pushedValue);

        execute(opcode, prefix);

        assertEquals(regValue, readShortFromMemory(SP));
        assertEquals(pushedValue, this.<Short>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("EX_Source")
    public void EX_SP_HL_IX_IY_do_not_change_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("EX_Source")
    public void EX_SP_HL_return_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 19 : 23, states);
    }
}