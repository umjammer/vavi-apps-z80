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


/*partial*/ class LD_IX_IY_plus_d_r_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_Source() {
        return Stream.of(
                arguments("A", "IX", (byte) 0x77, (byte) 0xDD),
                arguments("B", "IX", (byte) 0x70, (byte) 0xDD),
                arguments("C", "IX", (byte) 0x71, (byte) 0xDD),
                arguments("D", "IX", (byte) 0x72, (byte) 0xDD),
                arguments("E", "IX", (byte) 0x73, (byte) 0xDD),
                arguments("H", "IX", (byte) 0x74, (byte) 0xDD),
                arguments("L", "IX", (byte) 0x75, (byte) 0xDD),
                arguments("A", "IY", (byte) 0x77, (byte) 0xFD),
                arguments("B", "IY", (byte) 0x70, (byte) 0xFD),
                arguments("C", "IY", (byte) 0x71, (byte) 0xFD),
                arguments("D", "IY", (byte) 0x72, (byte) 0xFD),
                arguments("E", "IY", (byte) 0x73, (byte) 0xFD),
                arguments("H", "IY", (byte) 0x74, (byte) 0xFD),
                arguments("L", "IY", (byte) 0x75, (byte) 0xFD)
        );
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_d_r_loads_value_from_memory(String srcReg, String destPointerReg, byte opcode, byte prefix) {
        var address = Fixture.create(Short.TYPE);
        var offset = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);
        var newValue = Fixture.create(Byte.TYPE);
        var actualAddress = Add(address, ToSignedByte(offset));

        SetReg(destPointerReg, ToShort(address));
        SetReg(srcReg, newValue);
        ProcessorAgent.getMemory()[actualAddress] = oldValue;

        Execute(opcode, prefix, offset);

        assertEquals(newValue, ProcessorAgent.getMemory()[actualAddress]);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_d_r_do_not_modify_flags(String srcReg, String destPointerReg, byte opcode, byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_Source")
    public void LD_IX_IY_plus_d_r_return_proper_T_states(String srcReg, String destPointerReg, byte opcode, byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(19, states);
    }
}
