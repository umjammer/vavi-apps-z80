package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.Add;
import static konamiman.DataTypesAndUtils.NumberUtils.ToShort;
import static konamiman.DataTypesAndUtils.NumberUtils.ToSignedByte;
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
        var address = Fixture.create(Short.TYPE);
        var offset = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);
        var actualAddress = Add(address, ToSignedByte(offset));

        ProcessorAgent.getMemory()[actualAddress] = oldValue;
        SetReg(reg, ToShort(address));

        Execute(opcode, prefix, offset, newValue);

        assertEquals(newValue, ProcessorAgent.getMemory()[actualAddress]);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_does_not_modify_flags(String reg, byte opcode, byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_n_returns_proper_T_states(String reg, byte opcode, byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(19, states);
    }
}
