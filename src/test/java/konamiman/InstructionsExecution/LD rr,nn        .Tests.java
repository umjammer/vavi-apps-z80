package konamiman.InstructionsExecution;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.GetHighByte;
import static konamiman.DataTypesAndUtils.NumberUtils.GetLowByte;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class LD_rr_nn_tests extends InstructionsExecutionTestsBase {

    static Stream<Arguments> LD_rr_nn_Source() {
        return Stream.of(
                arguments("BC", (byte) 0x01, null),
                arguments("DE", (byte) 0x11, null),
                arguments("HL", (byte) 0x21, null),
                arguments("SP", (byte) 0x31, null),
                arguments("IX", (byte) 0x21, (byte) 0xDD),
                arguments("IY", (byte) 0x21, (byte) 0xFD)
        );
    };

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_loads_register_with_value(String reg, byte opcode, Byte prefix) {
        var oldValue = Fixture.create(Short.TYPE);
        var newValue = Fixture.create(Short.TYPE);

        SetReg(reg, oldValue);

        Execute(opcode, prefix, GetLowByte(newValue), GetHighByte(newValue));

        assertEquals(newValue, this.<Short>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_SP_nn_fires_FetchFinished_with_isLdSp_true(String reg, byte opcode, Byte prefix) {
        var eventFired = new AtomicBoolean(false);

        Sut.InstructionFetchFinished().addListener(e ->
        {
            eventFired.set(true);
            assertTrue((reg.equals("SP") && e.getIsLdSpInstruction()) | (!reg.equals("SP") && !e.getIsLdSpInstruction()));
        });

        Execute(opcode, prefix);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        AssertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = Execute(opcode, prefix);
        assertEquals(prefix != null ? 14 : 10, states);
    }
}

