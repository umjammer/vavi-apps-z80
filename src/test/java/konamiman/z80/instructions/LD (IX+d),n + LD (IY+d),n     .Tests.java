package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.add;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_IX_IY_plus_n_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_Source() {
        return Stream.of(
                arguments("IX", (byte) 0x36, (byte) 0xDD),
                arguments("IY", (byte) 0x36, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_loads_value_in_memory(String reg, byte opcode, byte prefix) {
        var address = fixture.create(Short.TYPE);
        var offset = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);
        var newValue = fixture.create(Byte.TYPE);
        var actualAddress = add(address, offset);

        processorAgent.writeToMemory(actualAddress, oldValue);
        setReg(reg, address);

        execute(opcode, prefix, offset, newValue);

        assertEquals(newValue, processorAgent.readFromMemory(actualAddress));
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_does_not_modify_flags(String reg, byte opcode, byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(19, states);
    }
}
