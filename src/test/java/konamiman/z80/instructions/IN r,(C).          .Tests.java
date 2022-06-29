package konamiman.z80.instructions;

import java.util.stream.Stream;

import konamiman.z80.utils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.z80.utils.NumberUtils.withBit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class IN_r_C extends InstructionsExecutionTestsBase {

    static Stream<Arguments> IN_r_C_Source() {
        return Stream.of(
                arguments("A", (byte) 0x78),
                arguments("B", (byte) 0x40),
                arguments("C", (byte) 0x48),
                arguments("D", (byte) 0x50),
                arguments("E", (byte) 0x58),
                arguments("H", (byte) 0x60),
                arguments("L", (byte) 0x68)
        );
    }

    static Stream<Arguments> IN_F_C_Source() {
        return Stream.of(
                arguments("F", (byte) 0x70)
        );
    }

    @ParameterizedTest
    @MethodSource("IN_r_C_Source")
    public void IN_r_C_reads_value_from_port(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var oldValue = fixture.create(Byte.TYPE);

        if (!reg.equals("C"))
            setReg(reg, value);
        registers.setA(oldValue);

        executeCase(opcode, portNumber, value);

        assertEquals(value, this.<Byte>getReg(reg));
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_SF_appropriately(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);

        executeCase(opcode, portNumber, (byte) 0xFE);
        assertEquals(1, registers.getSF().intValue());

        executeCase(opcode, portNumber, (byte) 0xFF);
        assertEquals(1, registers.getSF().intValue());

        executeCase(opcode, portNumber, (byte) 0);
        assertEquals(0, registers.getSF().intValue());

        executeCase(opcode, portNumber, (byte) 1);
        assertEquals(0, registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_ZF_appropriately(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);

        executeCase(opcode, portNumber, (byte) 0xFF);
        assertEquals(0, registers.getZF().intValue());

        executeCase(opcode, portNumber, (byte) 0);
        assertEquals(1, registers.getZF().intValue());

        executeCase(opcode, portNumber, (byte) 1);
        assertEquals(0, registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_resets_HF_NF(String reg, byte opcode) {
        assertResetsFlags(opcode, (byte) 0xED, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_does_not_change_CF(String reg, byte opcode) {
        var randomValues = fixture.create(byte[].class);
        var portNumber = fixture.create(Byte.TYPE);

        for (var value : randomValues) {
            registers.setCF(Bit.OFF);
            executeCase(opcode, portNumber, value);
            assertEquals(0, registers.getCF().intValue());

            registers.setCF(Bit.ON);
            executeCase(opcode, portNumber, value);
            assertEquals(1, registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_PF_as_parity(String reg, byte opcode) {
        var randomValues = fixture.create(byte[].class);
        var portNumber = fixture.create(Byte.TYPE);

        for (var value : randomValues) {
            executeCase(opcode, portNumber, value);
            assertEquals(parity[value & 0xff], registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_bits_3_and_5_from_result(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = withBit(withBit(((byte) 0), 3, 1), 5, 0);
        executeCase(opcode, portNumber, value);
        assertEquals(1, registers.getFlag3().intValue());
        assertEquals(0, registers.getFlag5().intValue());

        value = withBit(withBit(((byte) 0), 3, 0), 5, 1);
        executeCase(opcode, portNumber, value);
        assertEquals(0, registers.getFlag3().intValue());
        assertEquals(1, registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_returns_proper_T_states(String reg, byte opcode) {
        var portNumber = fixture.create(Byte.TYPE);
        var value = fixture.create(Byte.TYPE);
        var states = executeCase(opcode, portNumber, value);
        assertEquals(12, states);
    }

    private int executeCase(byte opcode, byte portNumber, byte value) {
        registers.setC(portNumber);
        setPortValue(portNumber, value);
        return execute(opcode, (byte) 0xED);
    }
}

