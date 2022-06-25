package konamiman.z80.instructions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class OUT_C_r extends InstructionsExecutionTestsBase {

    static Stream<Arguments> OUT_C_r_Source() {
        return Stream.of(
                arguments("A", (byte) 0x79),
                arguments("B", (byte) 0x41),
                arguments("C", (byte) 0x49),
                arguments("D", (byte) 0x51),
                arguments("E", (byte) 0x59),
                arguments("H", (byte) 0x61),
                arguments("L", (byte) 0x69)
        );
    }

    static Stream<Arguments> OUT_C_0_Source() {
        return Stream.of(
                arguments("0", (byte) 0x71)
        );
    }

    @ParameterizedTest
    @MethodSource({"OUT_C_r_Source", "OUT_C_0_Source"})
    public void OUT_C_r_writes_value_to_port(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = reg.equals("C") ? portNumber : fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);

        if (!reg.equals("0") && !reg.equals("C"))
            setReg(reg, value);
        setPortValue(portNumber, oldValue);

        Execute(opcode, portNumber, value);

        assertEquals(reg.equals("0") ? 0 : value, getPortValue(portNumber));
    }

    @ParameterizedTest
    @MethodSource({"OUT_C_r_Source", "OUT_C_0_Source"})
    public void OUT_C_r_does_not_change_flags(String reg, byte opcode) {
        assertDoesNotChangeFlags(opcode, (byte) 0xED);
    }

    @ParameterizedTest
    @MethodSource({"OUT_C_r_Source", "OUT_C_0_Source"})
    public void OUT_C_r_returns_proper_T_states(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var states = Execute(opcode, portNumber, value);
        assertEquals(12, states);
    }

    private int Execute(byte opcode, byte portNumber, byte value) {
        registers.setC(portNumber);
        setPortValue(portNumber, value);
        return execute(opcode, (byte) 0xED);
    }
}

