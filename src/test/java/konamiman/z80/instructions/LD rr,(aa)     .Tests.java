package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.toByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_rr_aa_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_rr_aa_Source() {
        return Stream.of(
                arguments("HL", (byte) 0x2A, null),
                arguments("DE", (byte) 0x5B, (byte) 0xED),
                arguments("BC", (byte) 0x4B, (byte) 0xED),
                arguments("SP", (byte) 0x7B, (byte) 0xED),
                arguments("IX", (byte) 0x2A, (byte) 0xDD),
                arguments("IY", (byte) 0x2A, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_aa_loads_value_from_memory(String reg, byte opcode, Byte prefix) {
        // TODO got error when 1 at (SP), when 0, 1, 2, 3 at (BC)
        var address = createAddressFixture((short) 0, (short) 1, (short) 2, (short) 3);
        var oldValue = fixture.create(Short.TYPE);
        var newValue = fixture.create(Short.TYPE);

        setReg(reg, oldValue);
        writeShortToMemory(address, newValue);

        execute(opcode, prefix, toByteArray(address));

        assertEquals(newValue, readShortFromMemory(address));
        assertEquals(newValue, this.<Short>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_r_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_aa_Source")
    public void LD_rr_r_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(reg.equals("HL") ? 16 : 20, states);
    }
}
