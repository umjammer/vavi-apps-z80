package konamiman.InstructionsExecution;

import java.util.stream.Stream;

import konamiman.DataTypesAndUtils.Bit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static konamiman.DataTypesAndUtils.NumberUtils.WithBit;
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
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var oldValue = Fixture.create(Byte.TYPE);

        if (!reg.equals("C"))
            SetReg(reg, value);
        Registers.setA(oldValue);

        Execute(opcode, portNumber, value);

        assertEquals(value, this.<Byte>GetReg(reg));
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_SF_appropriately(String reg, byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);

        Execute(opcode, portNumber, (byte) 0xFE);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, portNumber, (byte) 0xFF);
        assertEquals(1, Registers.getSF().intValue());

        Execute(opcode, portNumber, (byte) 0);
        assertEquals(0, Registers.getSF().intValue());

        Execute(opcode, portNumber, (byte) 1);
        assertEquals(0, Registers.getSF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_ZF_appropriately(String reg, byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);

        Execute(opcode, portNumber, (byte) 0xFF);
        assertEquals(0, Registers.getZF().intValue());

        Execute(opcode, portNumber, (byte) 0);
        assertEquals(1, Registers.getZF().intValue());

        Execute(opcode, portNumber, (byte) 1);
        assertEquals(0, Registers.getZF().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_resets_HF_NF(String reg, byte opcode) {
        AssertResetsFlags(opcode, (byte) 0xED, "H", "N");
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_does_not_change_CF(String reg, byte opcode) {
        var randomValues = Fixture.create(byte[].class);
        var portNumber = Fixture.create(Byte.TYPE);

        for (var value : randomValues) {
            Registers.setCF(Bit.OFF);
            Execute(opcode, portNumber, value);
            assertEquals(0, Registers.getCF().intValue());

            Registers.setCF(Bit.ON);
            Execute(opcode, portNumber, value);
            assertEquals(1, Registers.getCF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_PF_as_parity(String reg, byte opcode) {
        var randomValues = Fixture.create(byte[].class);
        var portNumber = Fixture.create(Byte.TYPE);

        for (var value : randomValues) {
            Execute(opcode, portNumber, value);
            assertEquals(Parity[value], Registers.getPF().intValue());
        }
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_sets_bits_3_and_5_from_result(String reg, byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = WithBit(WithBit(((byte) 0), 3, 1), 5, 0);
        Execute(opcode, portNumber, value);
        assertEquals(1, Registers.getFlag3().intValue());
        assertEquals(0, Registers.getFlag5().intValue());

        value = WithBit(WithBit(((byte) 0), 3, 0), 5, 1);
        Execute(opcode, portNumber, value);
        assertEquals(0, Registers.getFlag3().intValue());
        assertEquals(1, Registers.getFlag5().intValue());
    }

    @ParameterizedTest
    @MethodSource({"IN_r_C_Source", "IN_F_C_Source"})
    public void IN_r_C_returns_proper_T_states(String reg, byte opcode) {
        var portNumber = Fixture.create(Byte.TYPE);
        var value = Fixture.create(Byte.TYPE);
        var states = Execute(opcode, portNumber, value);
        assertEquals(12, states);
    }

    private int Execute(byte opcode, byte portNumber, byte value) {
        Registers.setC(portNumber);
        SetPortValue(portNumber, value);
        return Execute(opcode, (byte) 0xED);
    }
}

