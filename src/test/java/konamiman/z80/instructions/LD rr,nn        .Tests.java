package konamiman.z80.instructions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.getHighByte;
import static konamiman.z80.utils.NumberUtils.getLowByte;
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
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_loads_register_with_value(String reg, byte opcode, Byte prefix) {
        var oldValue = fixture.create(Short.TYPE);
        var newValue = fixture.create(Short.TYPE);

        setReg(reg, oldValue);

        execute(opcode, prefix, getLowByte(newValue), getHighByte(newValue));

        assertEquals(newValue, this.<Short>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_SP_nn_fires_FetchFinished_with_isLdSp_true(String reg, byte opcode, Byte prefix) {
        var eventFired = new AtomicBoolean(false);

        sut.instructionFetchFinished().addListener(e -> {
            eventFired.set(true);
            assertTrue((reg.equals("SP") && e.isLdSpInstruction()) | (!reg.equals("SP") && !e.isLdSpInstruction()));
        });

        execute(opcode, prefix);

        assertTrue(eventFired.get());
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_do_not_modify_flags(String reg, byte opcode, Byte prefix) {
        assertNoFlagsAreModified(opcode, prefix);
    }

    @ParameterizedTest
    @MethodSource("LD_rr_nn_Source")
    public void LD_rr_nn_returns_proper_T_states(String reg, byte opcode, Byte prefix) {
        var states = execute(opcode, prefix);
        assertEquals(prefix != null ? 14 : 10, states);
    }
}

